# Release note — event-shared-transport-accommodation

## New

- **Events can offer shared transport and shared accommodation.** A manager or
  event coordinator turns either offer on when creating or editing an event
  (form and inline edit, DRAFT or ACTIVE). Both are off by default.
- **Members choose the offers in their registration.** When an event offers
  shared transport and/or shared accommodation, the registration form and the
  edit-registration form show a checkbox for each. Choices can be changed for as
  long as the registration itself is editable.
- **Event detail shows opt-in counts.** For ACTIVE events, the event coordinator
  and users with `EVENTS:REGISTRATIONS` see "Společná doprava: N členů" /
  "Společné ubytování: M členů" for each enabled offer (shown even at 0).

## Behavioural change (BREAKING for the accommodation list)

- **"Seznam pro ubytování" now lists only members who chose shared
  accommodation**, not every registered member. The action — and its API
  endpoint, including the CSV download — is available only when the event has
  the shared-accommodation offer enabled; otherwise it is hidden and direct
  access is refused (403). The previous "all registered members" behaviour is
  gone.
- Turning an offer off is always allowed. Members' existing choices are kept but
  hidden; turning the offer back on makes them visible again.
