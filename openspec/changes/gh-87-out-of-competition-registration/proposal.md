## Why

GitHub issue #87 ("Chci se prihlasit do kategorie mimo soutez (mimo vekovou kategorii)", milestone `core`, labels `clen klubu`, `Events`, `hostujici clen`, `question`) asks for the ability to register for an event outside of the competition structure — i.e., to run the course without being ranked in any age category.

Today the `event-registrations` spec states: *"When the event has categories defined, the member MUST select a category."* (`Register for Event`). The registration form only offers the categories the event manager defined on the event; there is no way to say "I want to run, but I don't want to be scored" or "I'll run in an age category that isn't mine". For members who cannot make their normal age category for whatever reason (injury, accompanying a child, trying a longer course), the only options today are: don't register at all, or register into a category where they don't belong and distort the results.

Two things moved since the issue was filed and bear on the design: a registration now refers to its category by a stable identity rather than by name (`Registration Keeps Its Category Across Renames`, `EventCategoryId`), and registrations of ORIS-imported events are kept in step with ORIS — an entry made in ORIS appears in Klabis and takes its category from ORIS (`Registrations Entered In ORIS Appear In Klabis`). A registration with no category is therefore already a legal state (the category is preserved empty when its category is removed), but it currently means "category lost", not "out of competition".

The issue does not define the exact flow, which is reflected in the `question` label. Options include a dedicated "mimo soutěž" flag, a system-wide "P" (public) category, or allowing the member to pick an out-of-category option from the event registration form.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `event-registrations`: extend `Register for Event` and `List Event Registrations` with scenarios covering out-of-competition registrations (how the member opts in, how such registrations are rendered in the list).
- Possibly `events` / `event-categories`: depending on which option is chosen for expressing "out of competition" (flag on registration vs. a well-known category).

## Impact

**Affected specs:**
- `openspec/specs/event-registrations/spec.md` — `Register for Event` needs a new scenario letting the member register without choosing a competitive category (or with an explicit "out of competition" selection). `List Event Registrations` needs a scenario describing how the out-of-competition entries render (e.g., marked with a badge, sorted separately).
- Possibly `openspec/specs/events/spec.md` or `openspec/specs/event-categories/spec.md` — if a category-level implementation is chosen.

**Affected code (backend, event-registrations module):** the registration aggregate/command needs to accept an out-of-competition marker (boolean field on the registration, or a reserved category value). The `List Event Registrations` endpoint must return it. The ORIS registration sync adapter (`OrisEventRegistrationSyncAdapter`) reads the ORIS entry's category on the way in and declares no outward write capability, so the marker must not be lost or misread when an ORIS entry for such a member is matched with the Klabis registration.

**Affected code (frontend):** the "Registrovat na akci" modal/form gains an explicit opt-out control — most likely a checkbox "Mimo soutěž" that, when checked, hides/disables the category picker. The registrations list renders out-of-competition entries distinctly.

**APIs (REST):** additive — new optional field on the create-registration request and on each registration item in the list response.

**Dependencies:** none.

**Data:** new nullable column on the registrations table.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **What is the modelling approach — a flag, a category, or a distinct registration sub-type?**
   - Option A: boolean `outOfCompetition` on the registration. The form shows a checkbox; when checked, the category picker is hidden; the selected category stored on the registration is null. Simple, preserves existing category list.
   - Option B: a well-known reserved category value ("P", "mimo soutěž") that every event implicitly offers. Leverages the existing category field; no new data.
   - Option C: a reserved boolean on the event itself ("allow out-of-competition registrations") plus a flag on the registration. Event managers opt in per event.

2. **Can a member pick *any* of the defined categories (even ones not matching their age), or only the reserved out-of-competition slot?** The issue title mentions *"mimo vekovou kategorii"* — so both readings are possible. Recommend: exactly one reserved out-of-competition option, not free category choice, to keep the competitive classification clean.

3. **Is out-of-competition registration always allowed, or only when the event manager opts in?** ORIS-imported events often inherit out-of-competition availability from the upstream event. Recommend: always allowed on every event with categories. Confirm.

4. **Interaction with ORIS registration sync.** ORIS has a "mimo soutěž" concept (usually category "P"). Entries are now brought in from ORIS, and a Klabis change to a synchronised registration's category waits for a decision (`A Klabis Change To A Synchronised Registration Asks For A Decision`) because ORIS does not accept changes from Klabis. Decide how an out-of-competition marker behaves in that flow: is it a Klabis-only attribute (like the internal note and shared-service choices, which never cause a decision), or does it follow the ORIS category? Writing it to ORIS stays out of scope until Klabis can forward registrations at all.

5. **How does "mimo soutěž" interact with the SI card requirement?** Does the member still need to provide an SI card number (to pick up results / prove course completion)? Recommend: yes, the SI card stays required — the difference is only classification.

6. **Display in the registrations list:** should out-of-competition entries mix with competition entries (with a visual marker), or should they be grouped separately at the bottom? The `List Event Registrations` scenarios need to commit to one.

7. **Can a member toggle between competitive and out-of-competition by editing their registration?** Editing a registration shipped with #92 (`Edit Own Registration`, with entry points and prefill), so the toggle becomes one more editable field of that flow. Confirm.

8. **Guest members / `hostujici clen`.** Can a hosting member register as out-of-competition as freely as a regular member? Recommend: yes, same rules. Confirm.
