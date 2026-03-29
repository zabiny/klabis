## Context

Events table currently shows 5 columns (date, name, location, organizer, status). Users need more information at a glance: external website link, registration deadline, coordinator name, and registration action — without navigating to detail page.

Current state:
- `EventSummaryDto` returns: id, name, eventDate, location, organizer, status
- List endpoint adds only `self` link per item (to detail) — no coordinator link, no registration affordance
- `areRegistrationsOpen()` logic: `status == ACTIVE && eventDate.isAfter(LocalDate.now())`
- No `registrationDeadline` field exists in domain
- `@HasAuthority` field-level security is already implemented (Jackson `BeanSerializerModifier`)

## Goals / Non-Goals

**Goals:**
- Add `registrationDeadline` (optional LocalDate) to Event domain with business logic for closing registrations
- Extend list API response with websiteUrl, registrationDeadline, coordinator link, registration affordance
- Make `status` field visible only to users with `EVENTS:MANAGE` permission
- Update frontend table to show all requested columns
- Show `registrationDeadline` on event detail page (read-only display + inline edit)

**Non-Goals:**
- Batch registration from list view
- Filtering by registrationDeadline

## Decisions

### 1. Registration deadline as domain field on Event aggregate

`registrationDeadline` is an optional `LocalDate` on Event. When set and passed, `areRegistrationsOpen()` returns false even if event is ACTIVE and in the future.

Updated logic:
```
areRegistrationsOpen() = status == ACTIVE
    && eventDate.isAfter(now)
    && (registrationDeadline == null || registrationDeadline.isAfter(now))
```

**Alternative considered:** Separate "registration window" concept with open/close dates — rejected as over-engineering for current needs.

### 2. Extend EventSummaryDto with new fields

Add `websiteUrl`, `registrationDeadline` to `EventSummaryDto`. Apply `@HasAuthority(EVENTS_MANAGE)` on `status` field so it is serialized as `null` (omitted via `@JsonInclude(NON_NULL)`) for regular users.

### 3. Coordinator link on list items via _links.coordinator

Add `_links.coordinator` HATEOAS link to each event summary item in the list (same pattern as detail page). Frontend resolves coordinator name by following the link — consistent with existing HAL pattern.

**Alternative considered:** Adding `coordinatorName` directly to DTO — rejected because it crosses bounded context (member data in events DTO) and requires join query.

### 4. Registration affordance on list item self link

For each event in the list, add `registerForEvent`/`unregisterFromEvent` affordance on the item's self link — same logic as detail page (`addLinksForEvent`). This requires loading full Event (with registrations) for each list item.

**Performance note:** This means loading Event aggregates (not just summary projections) for the list. Acceptable for club-scale data (tens of events per page).

### 5. Frontend table columns

```
| Datum | Název | Místo | Pořadatel | Web | Uzávěrka | Koordinátor | Status* | Akce |
```
\* Status visible only when API returns it (field-level security)

- **Web**: ExternalLink icon, clickable if `websiteUrl` present
- **Uzávěrka**: Formatted date or empty
- **Koordinátor**: Name fetched from `_links.coordinator`, clickable to member detail
- **Akce**: HalFormButton for registerForEvent/unregisterFromEvent affordance

### 6. ORIS import — map EntryDate1 to registrationDeadline

ORIS `EventDetails` has three entry deadline fields: `EntryDate1`, `EntryDate2`, `EntryDate3` (all `ZonedDateTime`). Map `EntryDate1` (primary registration deadline) to `registrationDeadline`. Since `registrationDeadline` is `LocalDate` and ORIS provides `ZonedDateTime`, extract the date part only. If `EntryDate1` is null, `registrationDeadline` remains null.

Change is in `EventManagementServiceImpl.importEventFromOris()` — add the deadline to `Event.createFromOris()` factory method call.

### 7. Event detail page — registrationDeadline display

`EventDto` already includes all event fields. Adding `registrationDeadline` to the DTO is covered by task 2.1. On the frontend, `EventDetailPage` uses the inline editing pattern (`enrichTemplateWithReadOnlyFields` + `HalFormDisplay` with `customLayout`). The new field follows the same pattern as `websiteUrl`: displayed as read-only `DetailRow` with formatted date, editable via `ri('registrationDeadline')` when in edit mode. No structural change to the page — just one more `DetailRow`.

## Risks / Trade-offs

- **[Performance] Loading full aggregates for list** → Acceptable for club scale. If needed later, can add dedicated projection query.
- **[N+1 for coordinator names] Frontend fetches coordinator for each row** → Can be mitigated with TanStack Query caching (same coordinator appears in multiple events). Alternative: batch fetch — deferred to future if needed.
- **[DB migration] New column** → Simple `ALTER TABLE ADD COLUMN registration_deadline DATE NULL`. No data migration needed (existing events get null = no deadline).
