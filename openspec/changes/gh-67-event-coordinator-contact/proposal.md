## Why

GitHub issues #67 ("Chci kontaktovat vedoucího akce", milestone `core`) and its duplicate #37 ("Chci kontaktovat vedoucího akce, ...", milestone `core`) both ask for a way for members to contact the event coordinator directly from the event context.

Today the `events` spec supports assigning a coordinator as a member reference (`Create Event`, `Get Event Detail`, `Events Table Display`) and the events list displays the coordinator's name as a clickable link to the member detail page. From the member detail page, one can see the coordinator's contact details (email, phone per the `members` spec `Member Detail` requirement, visible to authenticated members with `MEMBERS:READ`).

There are two problems with this indirect path:

- **Discoverability.** Members who want to contact the coordinator have to navigate to the coordinator's profile page and hunt for contacts among all other member fields. The path is not obvious from the event context.
- **Guest members.** Hosting members (`hostujici clen` label appears on #67 and #37) may not have `MEMBERS:READ` permission at all, meaning clicking the coordinator link produces an access-denied result. They currently have no way to reach the coordinator.
- **Non-registered website visitors.** Not yet a spec concern because the application is authenticated-only, but #37 is tagged `clen klubu` AND `hostujici clen` — at a minimum both of those personas should be able to contact the event coordinator.

No dedicated "kontakt na vedoucího akce" affordance exists on the event detail page or in the events list.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `events`: extend `Event Detail Page` (and optionally `Events Table Display`) with a coordinator-contact affordance that is reachable without traversing the member detail page.

## Impact

**Affected specs:**
- `openspec/specs/events/spec.md` — `Event Detail Page` gains scenarios for showing the coordinator's contact details or an action to contact them directly. `Events Table Display` may gain a scenario for a row-level "contact coordinator" action.

**Affected code (backend, events module):** the event detail and list representations must expose (or affordance-wrap) the coordinator's contact information in a way that does not depend on the caller having `MEMBERS:READ`. The exact mechanism (embed contacts in event DTO vs. add a dedicated HAL-Forms affordance that opens a "send message" dialog vs. `mailto:` link) is an open design question.

**Affected code (frontend):** event detail page gains a contact button/section. Events list table may gain a row-level contact action.

**APIs (REST):** additive. If contacts are embedded, the event detail response grows. If only a `mailto:` link is exposed, the change is minimal.

**Dependencies:** none.

**Data:** none.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **What does "contact" mean in scope — email only, or email + phone?**
   - Option A: just a `mailto:` link using the coordinator's registered email. Zero backend work beyond field exposure.
   - Option B: both email and phone visible in an "event info → coordinator" block on the detail page (and optionally on row hover in the list).
   - Option C: a server-side "send message" flow that forwards a message without disclosing the raw email (requires a new email-service capability, out of scope for MVP likely).

2. **Which personas see the coordinator's contacts?** Today `MEMBERS:READ` guards member detail. For event coordinator contact:
   - (a) Everyone authenticated — the role of "coordinator on an active event" makes the contact implicitly public among members.
   - (b) Same rules as `members` (`MEMBERS:READ` required) — status quo for the data, just exposed more conveniently.
   - (c) A dedicated `EVENTS:READ_COORDINATOR_CONTACT` permission.

3. **Do we also want a "contact coordinator" affordance for events imported from ORIS where the coordinator field is not set?** ORIS events often have an organizer contact in the ORIS upstream but not in our system. Out of scope unless we add ORIS-sourced contact fallback.

4. **Should this change also cover contacting the event *deputy* coordinator once that field exists (see proposal `add-event-deputy-coordinator`, issue #83)?** Recommend: yes — make the contact affordance list all coordinators (primary + deputies) consistently. Confirm or defer to a follow-up.

5. **#67 vs. #37 deduplication.** Both issues describe the same feature. Recommend marking #37 as "duplicate of #67" in GitHub after this proposal lands. Confirm.

6. **Is a click-to-call (`tel:`) link in scope, or only email?** Frontend-only decision but spec scenarios need to reflect it.

7. **Does the club want the contact affordance on the event detail page only, or also in the events list table (one action per row)?** The per-row variant clutters the list; the detail-only variant requires one extra click. Recommend: detail-only, confirm.
