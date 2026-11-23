# Spec Delta

## MODIFIED Requirements

### Requirement: Shared Transport And Accommodation Count Summary On The Event Detail Page

On the detail page of an ACTIVE event, for each offer that is turned on, the event coordinator and users with the EVENTS:REGISTER_MEMBER authority SHALL see how many registered members have asked to use that offer. The summary shows a separate line per turned-on offer and is shown even when the count is zero. Members without the EVENTS:REGISTER_MEMBER authority who are not the coordinator do not see the summary. The summary is not shown for DRAFT, FINISHED, or CANCELLED events, nor for an offer that is turned off.

#### Scenario: Coordinator sees the count summary for both offers

- **GIVEN** an ACTIVE event with both the shared transport and shared accommodation offers turned on
- **WHEN** the event coordinator opens the event detail page
- **THEN** the page shows a line "Společná doprava: N členů" with the number of registered members who asked for shared transport
- **AND** a line "Společné ubytování: M členů" with the number of registered members who asked for shared accommodation

#### Scenario: User with EVENTS:REGISTRATIONS sees the count summary

- **GIVEN** an ACTIVE event with at least one offer turned on
- **WHEN** a user with the EVENTS:REGISTER_MEMBER authority opens the event detail page
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
- **WHEN** an authenticated member who is not the coordinator and has no EVENTS:REGISTER_MEMBER authority opens the event detail page
- **THEN** no count summary is shown

#### Scenario: Summary is not shown for a non-active event

- **GIVEN** a DRAFT event with an offer turned on
- **WHEN** the event coordinator opens the event detail page
- **THEN** no count summary is shown
