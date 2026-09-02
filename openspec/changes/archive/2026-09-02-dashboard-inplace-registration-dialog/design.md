## Context

The dashboard "Končící přihlášky tento týden" widget (`UpcomingDeadlinesWidget.tsx`) renders its own generic modal wrapping `HalFormDisplay` for the "Přihlásit se" action. Every other registration entry point (event detail page toolbar, events list rows, registration-row edit) uses the customized `EventRegistrationDialog` (SI-chip prefill from the member profile, event/deadline context header, member chip in edit mode, shared-service checkboxes gated on the event's offers).

The widget's rows come from the upcoming-deadlines endpoint, which returns `EventSummaryDto` items. That representation carries `newRegistration` link and `registerForEvent` template (attached by `EventSummaryPostprocessor`) but not the shared-offer flags, so a dialog opened from a list row cannot honor the `event-registrations` rule "choice offered only when the event has the matching offer turned on" (the dialog hides a checkbox unless the flag is strictly `true`). The events list rows use the same `EventSummaryDto`, so the gap affects the events list flow identically.

The dashboard spec currently contradicts itself: the requirement text mandates the in-place registration form, while the last scenario still describes navigation to the event detail page.

## Goals / Non-Goals

**Goals:**

- The dashboard widget opens the same customized `EventRegistrationDialog` as the events list, in place, with prefill and offer-aware checkboxes.
- The shared-offer flags are available wherever the registration dialog is opened from a list row.
- Dashboard spec text matches the implemented behavior (in-place dialog, no navigation).

**Non-Goals:**

- No change to `event-registrations` requirements, registration domain rules, or the `newRegistration` prefill endpoint.
- No change to the events list endpoint's query/filter behavior.
- No redesign of the widget row layout or the "Zobrazit všechny" footer link.

## Decisions

- **Swap the widget's modal for `EventRegistrationDialog`** (`mode="new"`, template from the row's `_templates.registerForEvent`, `prefillHref` from the row's `newRegistration` link, `onRegistered` refetches the widget's query so the freshly registered event disappears on the next load). Alternative considered: fetch the event detail on open and reuse its data — rejected, extra request and duplicated context when the summary row already carries everything needed once flags are added.
- **Expose `sharedTransportEnabled`/`sharedAccommodationEnabled` on `EventSummaryDto`** (OpenAPI spec + `EventSummaryDtoConverter`, same plain-boolean shape as on `EventDto`). Alternatives considered: keep the flags only on the detail representation and navigate to the detail page instead of an in-place dialog — rejected, contradicts the spec requirement and the customized-dialog UX; embed offer info in the `newRegistration` prefill response — rejected, the dialog reads offer flags from the `event` prop everywhere else and the prefill response shape is member-scoped.
- **Extend `UpcomingDeadlineItem`** with the raw fields the dialog needs (`deadlines` array for the relevant-deadline chip, `location`, both offer flags) instead of only the pre-picked single deadline string; the row display keeps using the picked relevant deadline.
- **`EventsPage` needs no code change** — it already passes the list-row event into the dialog; the flags start flowing once the summary DTO carries them.

## Risks / Trade-offs

- [Summary payloads grow by two booleans for every list consumer] → Negligible size; fields are plain scalars, no per-caller authority involved (mirrors `EventDto`).
- [Dialog opened from a row whose `registerForEventTemplate` is stale after a refetch] → The widget holds the open dialog's target in component state, so a background refetch leaves the dialog open with its pre-refetch snapshot until close/submit; the next open always uses a fresh row.
- [Registration from the dashboard now submits shared-service choices] → Same payload semantics as the events list flow; backend applies the same offer-gated retention rules.

## Migration Plan

Single deploy: backend contract addition is additive (new optional fields), frontend ships in the same release. Rollback is a plain revert.

## Open Questions

(none)
