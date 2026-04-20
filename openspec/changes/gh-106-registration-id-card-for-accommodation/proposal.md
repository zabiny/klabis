## Why

GitHub issue #106 ("Potřebují čísla OP/pasů na ubytování", milestone `MVP`, labels `question`, `vedoucí akce`) asks for the event coordinator to be able to collect ID-card / passport numbers of members who have signed up for accommodation — so the coordinator can hand the list to the hotel/hostel at check-in.

Today the relevant specs do this:

- `members` `Identity Card, Licenses, and Supplementary Fields` stores an ID-card number + validity date on the member profile. Members update it themselves; there is per-field visibility (likely `MEMBERS:READ` or similar) — the ID card is not freely visible to every other member.
- `event-registrations` is silent on accommodation and on ID cards. Registrations carry only SI card number, category, and timestamp.
- There is no accommodation concept at the event level today (prerequisite: supplementary services, see proposal #112 / issue #112).

The gap:
1. Even though an ID card is already stored on the member profile, the event coordinator has no one-click way to produce a list of ID cards for the members who signed up for accommodation on a specific event — they would have to open every registered member's profile manually, and they likely don't have permission.
2. Once supplementary services / accommodation exist (see #112, #103, #105), the coordinator needs to pull ID-card data for just the subset of registrants who selected accommodation.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `event-registrations`: extend the "registrations list for an event" context so coordinators can access ID-card data for accommodation-registered members.
- `members`: extend the field-level authorization on `Identity Card` so an event coordinator has read access to the ID cards of members registered to their event (scoped read). Or alternatively, leave the member field alone and surface the data through a coordinator-only registration-list view.

## Impact

**Affected specs:**
- `openspec/specs/event-registrations/spec.md` — new scenarios: "Coordinator exports accommodation list with ID cards". Access rules: only the event coordinator (and possibly deputy coordinator from #83) can see ID-card data for their event's registrants; only for registrations that have an accommodation service selected.
- `openspec/specs/members/spec.md` — may need an updated access-control scenario on the ID-card field: today visible with `MEMBERS:READ`; this proposal would add a scoped role ("I am the coordinator of an event this member registered to") that also grants read access.
- Possibly a new requirement about "Accommodation list export" (PDF/CSV) on `event-registrations`.

**Affected code (backend):**
- A new endpoint or view on the event's registration list that includes ID-card fields when the caller is the event coordinator AND the registration has accommodation.
- Field-level authorization: extend whatever guard today gates the ID card, to honor the coordinator-scope case.

**Affected code (frontend):**
- Event detail → registrations list: for the coordinator, an additional tab/view "Ubytování" showing members with accommodation selected, their first/last name, birthdate, ID card number, ID card validity, and optionally home address.
- Export button (CSV or PDF) for the accommodation list.

**APIs (REST):** additive — new endpoint `GET /api/events/{id}/accommodation-list` or an extended registration-list representation.

**Dependencies:**
- Issue #112 (event supplementary services) — accommodation must be representable as a service selectable at registration.
- Issue #102 (members pick supplementary services at registration) — the selection itself.

Without those, this proposal is abstract: we can still define access rules for ID cards, but the "only for accommodation-registered members" clause has no meaning yet.

**Data:** none new per this proposal; depends on accommodation-service data from #112/#102.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Scope — just the access rule, or the full export flow?**
   - Option A: only add the access-rule requirement; defer export UX to a separate issue.
   - Option B: include the export (CSV + PDF) in this proposal.

   Recommend: Option B — the acceptance criterion is practical ("dostat data do hotelu"), so a working export should ship with the proposal. Confirm.

2. **Dependency ordering.** This proposal cannot produce tasks until #112 (supplementary services) is defined. Options:
   - Block this proposal's design on #112 landing.
   - Parameterize the "accommodation service" reference (this proposal assumes there will be a service named "ubytování" or marked as accommodation; lands with `TODO`'d placeholders).

   Recommend: block. Confirm.

3. **Coordinator vs. deputy vs. arbitrary event manager.** Who exactly can see the ID cards?
   - (a) Only the event coordinator (single person).
   - (b) Coordinator + deputy coordinator (pending #83).
   - (c) Anyone with `EVENTS:MANAGE`.

   Recommend: (b) — coordinators and deputies of that specific event. `EVENTS:MANAGE` alone is too broad. Confirm.

4. **Fields included on the accommodation list.**
   - Required: first name, last name, date of birth, ID card number, ID card validity.
   - Nice to have: home address (for tax-residence declaration needed by some hotels).
   - Should nationality be included? Recommend yes (for foreign hostels).
   - Should birth number (rodné číslo) be included? The hotel asks for "datum narození" typically, not birth number. Recommend no — omit birth number.

5. **What if a member has no ID card on their profile?** Blank row with a visible "chybí OP" flag, so the coordinator can chase them. Recommend that behavior. Confirm.

6. **Export format.**
   - CSV: trivial.
   - PDF: needs a template. Low-fi: list of rows in a table.
   - Excel: nice-to-have.

   Recommend: CSV for MVP, PDF as follow-up.

7. **Encryption / audit.** The ID card is stored encrypted at rest (per birth-number scenario). When exported, the CSV is plaintext. Should the system log who exported the list and when? Recommend: yes, audit log entry for each export.

8. **GDPR.** This is explicit personal data. The access-rule scenarios need to affirm that only event coordinators/deputies of that specific event may see the data, not all members and not all managers. The proposal must state this explicitly.

9. **Multi-event accommodation (e.g., weekend with two linked events, see #104).** Shared accommodation across events is a separate issue (#104); this proposal scopes to a single event only. Confirm.

10. **UI: is the accommodation list visible in the existing registrations list, or as a separate tab?** Recommend: separate tab — the general registrations list has different audience/permissions.
