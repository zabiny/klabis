## Why

GitHub issue #112 ("Chci mít možnost přepsat informace v importovaných akcích (vcetne doplnkovych sluzeb)", milestone `MVP`, labels `přihlašovatel`, `question`) has two parts:

1. **Overwrite any ORIS-imported event information locally** — a manager should be able to edit fields of an ORIS-imported event and have the local version take precedence over subsequent ORIS syncs.
2. **Supplementary services** (*doplňkové služby*) — the event should support extra services (accommodation, transport, dinner, T-shirt, …) that members can pick during registration.

### Part 1 is delivered

The synchronisation engine (`data-synchronization`, archived as `add-bidirectional-sync-engine`) and the `events` requirements built on it settle this part completely:

- `Update Event` lets a manager edit a DRAFT or ACTIVE ORIS-imported event through the standard flow.
- `A Manager's Edit To An ORIS Field Is Never Silently Overwritten`: an edit to an ORIS-owned field (name, date, location, organiser, website, registration deadlines, ranking, base entry fee) is never sent to ORIS and never discarded by the next synchronisation. The event stops being synchronised and asks the manager to decide, showing what differs; the manager either takes the ORIS values or keeps the deliberate difference (a later change in ORIS then asks again).
- Fields ORIS does not own (category fee override, manually added categories, event type, shared transport / accommodation offers) are never subject to a decision.
- `ORIS-Imported Events Are Kept In Step Automatically` and `Bulk Synchronize ORIS-Imported Upcoming Events` define what happens on re-sync.

The per-field "locally overridden" flag and "reset to ORIS" action that this proposal originally sketched are not needed; the decision-based model replaced them. The open questions on override granularity, on blocking auto-import and on the "upraveno" badge are closed by those requirements.

### Part 2 is partly delivered, partly owned elsewhere

- **Shared transport and shared accommodation** shipped (`event-shared-transport-accommodation`): a manager or coordinator turns each offer on per event, the member ticks `Chci využít společnou dopravu` / `Chci využít společné ubytování` when registering or editing a registration, the event detail shows count summaries, and the coordinator gets the accommodation list with CSV export (`Generate Accommodation List for Event Registrations`, which also closes the data need of issue #106). These are yes/no offers without name, price, capacity or deadline.
- **General supplementary services** (named services with a price, selected by id at registration, price shown as a breakdown) are not modelled yet. Their design is owned by the open change `event-registration-pricing` (capabilities `event-supplementary-services` and `event-registration-pricing`; service with stable id, optional `orisId`, name and price; selection stored on the registration). This proposal no longer duplicates that design.

## Capabilities

### New Capabilities

<!-- None here. `event-supplementary-services` is introduced by `event-registration-pricing`. -->

### Modified Capabilities

<!-- None remaining from this issue's scope. -->

## Impact

This proposal has no remaining scope of its own. It is kept as the record of issue #112 and should stay open only until `event-registration-pricing` covers the remaining services requirements below, then be closed together with the issue.

What is still open from the issue, and where it is tracked:

| Need from #112 | Status | Where |
|---|---|---|
| Edit ORIS-imported event locally, keep edits across sync | Done | `events`, `data-synchronization` |
| Yes/no shared transport and accommodation | Done | `events`, `event-registrations` |
| Named services with price, selectable at registration | Open | `event-registration-pricing` |
| Service capacity (limited places, issue #103) | Open, not covered by any change | see Open Questions |
| Service-specific deadline (T-shirt orders close earlier) | Open, not covered by any change | see Open Questions |
| Import services from ORIS | Open | `event-registration-pricing` keeps an optional `orisId` on a service; importing from ORIS is not in its scope |

**Dependencies:** `event-registration-pricing`.

**Data, API, code:** none from this proposal.

## Open Questions

1. **Close this proposal?** With part 1 delivered and part 2 owned by `event-registration-pricing`, nothing is left that needs its own change. Close it (and note on the issue which parts are delivered and which sit in `event-registration-pricing`), or keep it open to carry the two uncovered needs below?

2. **Service capacity and per-service deadline.** `event-registration-pricing` models name and price only. The original issue wanted capacity (accommodation places, #103) and an optional own deadline per service. Add them to `event-registration-pricing`, give them a follow-up change, or drop them for the MVP? The shared accommodation offer has no capacity either, so the question is already live for it.

3. **How do the shared offers and the services relate?** A priced "Ubytování" service from `event-registration-pricing` and the `shared accommodation` yes/no offer describe overlapping things. Either services replace the offers (migrating existing choices and the accommodation list), or the offers stay as the coordination feature (who needs a ride or a bed) and services stay the priced feature. This must be decided in `event-registration-pricing`, but it originates here, so it is recorded here too.
