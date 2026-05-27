/## Context

The `member-accounts` capability already provides a unified transaction dialog (deposit/charge) reachable from the member list and from the member's full account page. The dialog operates over a member's financial account via existing API endpoints; rendering and authorization are driven by HAL+FORMS affordances exposed for `FINANCE:MANAGE` holders.

The `event-registrations` capability lists members registered for an event. For finance managers, the registrations list of an event is the natural workspace to charge entry fees (startovné) or refund cancellations: only the participants of the event are present, in one place, with their identity already shown.

Today, a finance manager who wants to charge an entry fee must leave the event page, navigate to each member's account, find the right member, open the dialog, and record the transaction — repeated per registration. This is slow and error-prone.

## Goals / Non-Goals

**Goals:**
- Allow a finance manager to record a deposit or a charge on the account of any member listed in an event registrations list, without leaving that list.
- Reuse the existing unified transaction dialog (UI component) and the existing deposit/charge API endpoints (no new backend transaction semantics).
- Prefill the transaction note with the event name so the resulting account history is self-describing.

**Non-Goals:**
- Bulk charging across many registrations at once (out of scope for this change).
- Automatic charging on registration (no event-based pricing model introduced here).
- Changes to the account semantics (overdraft, reversal rules, append-only history) — all reused as-is.
- Changing who can register / unregister members.

## Decisions

### Decision 1: Reuse the existing unified transaction dialog and endpoints
Render the same unified transaction dialog component already used in `member-accounts`. Submitting calls the existing deposit/charge endpoints on the target member's account.

**Why:** Behavioral and visual consistency (members hear about a single "transaction dialog"). No new API surface. The dialog already handles tab control, identity header, balance display, and authorization fallbacks.

**Alternatives considered:**
- A new event-scoped transaction endpoint (e.g., `POST /events/{id}/registrations/{id}/charge`). Rejected — adds API surface without new semantics, complicates auditing (the canonical record still lives on the account).

### Decision 2: Expose the action as a HAL affordance on each registration row
Each registration in the registrations list resource SHALL carry an affordance (e.g., `recordTransaction`) that points to the existing transaction endpoint on the registered member's account. The frontend renders the action conditional on the presence of the affordance.

**Why:** Aligns with the project's HATEOAS-first rule (frontend renders from server metadata). `FINANCE:MANAGE` is enforced server-side: holders see the affordance, others do not. No client-side authorization branching.

### Decision 3: Prefill the transaction note with the event name
When the dialog is opened from a registrations list, the note field SHALL be prefilled with the event name (e.g., `Startovné: Mistrovství ČR 2026`). The user can edit or clear the value before submitting.

**Why:** The most common transaction recorded in this flow is the entry fee for the specific event. Prefilling reduces typing and produces consistent, searchable history entries. Editing remains free because the action also supports refunds, late fees, or per-member adjustments.

### Decision 4: Action visibility and authorization
The action and its affordance SHALL be exposed only when the viewer holds `FINANCE:MANAGE`. The rule mirrors the existing member-list inline dialog rule from `member-accounts` — same authority, same UX shape.

**Why:** Single, well-understood authority for all transaction recording. No new permission introduced.

### Decision 5: Stay on the registrations list after submission
After a successful submission, the dialog closes, the registrations list remains the active page, and the row context is preserved (no navigation away). Account balance changes are not displayed in the registrations list itself.

**Why:** Finance managers typically process multiple registrations in sequence; staying on the list keeps that flow uninterrupted. The balance shown inside the dialog header (loaded when the dialog opens) is sufficient feedback for the single transaction just recorded.

## Risks / Trade-offs

- **Risk: Note prefill creates noisy history if the manager forgets to edit during a refund.** → Mitigation: the prefill is a sensible default for the dominant use case (charging); reversals already produce a distinct, marked entry, and the note can be edited before submit.
- **Risk: Finance managers may expect bulk-charge semantics from this surface.** → Mitigation: out of scope; if bulk charging becomes needed, it warrants its own capability (event-based pricing). Document explicitly as a non-goal here.
- **Trade-off: No event-scoped audit trail.** Transactions live only on the member account, not linked back to the originating registration. Acceptable because the prefilled note carries the event name; tighter linking can be added later if reporting requires it.
- **Risk: Dialog is opened for a member with insufficient operations permitted.** → Mitigation: dialog reuses existing behavior — tabs/forms reflect permitted operations; if none, the affordance is not emitted at all.
