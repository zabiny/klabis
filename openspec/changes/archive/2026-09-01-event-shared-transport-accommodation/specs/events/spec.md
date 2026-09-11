## ADDED Requirements

### Requirement: Shared Transport And Shared Accommodation Options On An Event

A manager or event coordinator SHALL be able to turn on, per event, an offer of shared transport and an offer of shared accommodation. The two offers are independent: either, both, or neither can be turned on. Both are off by default. The offers can be turned on or off while the event is in DRAFT or ACTIVE status, and cannot be changed once the event is FINISHED or CANCELLED.

Turning an offer off is always allowed, even when members have already asked to use it. Those member choices are kept but stop being shown anywhere; turning the offer back on makes them visible again.

#### Scenario: Manager turns on shared transport when creating an event

- **WHEN** a user with EVENTS:MANAGE permission creates an event and ticks "Nabídnout společnou dopravu"
- **THEN** the event is created with the shared transport offer turned on
- **AND** the shared accommodation offer stays off unless also ticked

#### Scenario: Event is created with both offers off by default

- **WHEN** a user with EVENTS:MANAGE permission creates an event without touching the shared transport or shared accommodation fields
- **THEN** the event is created with both offers off

#### Scenario: Manager turns on shared accommodation on an existing event

- **GIVEN** an ACTIVE event with the shared accommodation offer off
- **WHEN** a user with EVENTS:MANAGE permission edits the event and ticks "Nabídnout společné ubytování"
- **THEN** the event is updated with the shared accommodation offer turned on

#### Scenario: Coordinator changes the offers without EVENTS:MANAGE

- **GIVEN** a DRAFT or ACTIVE event that lists the current user as a coordinator
- **WHEN** the coordinator edits the event and changes the shared transport or shared accommodation offer
- **THEN** the event is updated with the new setting
- **AND** no EVENTS:MANAGE permission is required

#### Scenario: Inline edit on the event detail page includes the two offers

- **WHEN** a manager or coordinator edits an event inline on the detail page
- **THEN** the form shows a "Nabídnout společnou dopravu" tick box and a "Nabídnout společné ubytování" tick box reflecting the current settings

#### Scenario: Turning off an offer that members have already chosen

- **GIVEN** an ACTIVE event with the shared accommodation offer on and several members who asked to use shared accommodation
- **WHEN** a manager or coordinator turns the shared accommodation offer off
- **THEN** the event is updated with the offer off
- **AND** no error is shown
- **AND** the accommodation choices made by those members are retained but no longer shown

#### Scenario: Turning an offer back on restores earlier choices

- **GIVEN** an event where the shared accommodation offer was turned off after members had chosen it
- **WHEN** a manager or coordinator turns the shared accommodation offer back on
- **THEN** the earlier accommodation choices are shown again in the count summary and the accommodation list

#### Scenario: Offers cannot be changed on a finished or cancelled event

- **WHEN** a user attempts to change the shared transport or shared accommodation offer on a FINISHED or CANCELLED event
- **THEN** the system shows an error that the event can no longer be modified

### Requirement: Shared Transport And Accommodation Count Summary On The Event Detail Page

On the detail page of an ACTIVE event, for each offer that is turned on, the event coordinator and users with the EVENTS:REGISTRATIONS authority SHALL see how many registered members have asked to use that offer. The summary shows a separate line per turned-on offer and is shown even when the count is zero. Members without the EVENTS:REGISTRATIONS authority who are not the coordinator do not see the summary. The summary is not shown for DRAFT, FINISHED, or CANCELLED events, nor for an offer that is turned off.

#### Scenario: Coordinator sees the count summary for both offers

- **GIVEN** an ACTIVE event with both the shared transport and shared accommodation offers turned on
- **WHEN** the event coordinator opens the event detail page
- **THEN** the page shows a line "Společná doprava: N členů" with the number of registered members who asked for shared transport
- **AND** a line "Společné ubytování: M členů" with the number of registered members who asked for shared accommodation

#### Scenario: User with EVENTS:REGISTRATIONS sees the count summary

- **GIVEN** an ACTIVE event with at least one offer turned on
- **WHEN** a user with the EVENTS:REGISTRATIONS authority opens the event detail page
- **THEN** the count summary is shown for each turned-on offer

#### Scenario: Summary line is shown even when nobody has chosen the offer

- **GIVEN** an ACTIVE event with the shared transport offer turned on and no member has asked to use it
- **WHEN** the event coordinator opens the event detail page
- **THEN** the page shows "Společná doprava: 0 členů"

#### Scenario: Only turned-on offers appear in the summary

- **GIVEN** an ACTIVE event with the shared transport offer on and the shared accommodation offer off
- **WHEN** the event coordinator opens the event detail page
- **THEN** the summary shows the shared transport line
- **AND** no shared accommodation line is shown

#### Scenario: Regular member does not see the count summary

- **GIVEN** an ACTIVE event with an offer turned on
- **WHEN** an authenticated member who is not the coordinator and has no EVENTS:REGISTRATIONS authority opens the event detail page
- **THEN** no count summary is shown

#### Scenario: Summary is not shown for a non-active event

- **GIVEN** a DRAFT event with an offer turned on
- **WHEN** the event coordinator opens the event detail page
- **THEN** no count summary is shown
