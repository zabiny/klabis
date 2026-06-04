## MODIFIED Requirements

### Requirement: Event Ranking (Series)

An event MAY have a **Ranking facet** that identifies which competition series or ranking level the event belongs to (e.g. Regional Ranking, Ranking B, Czech Cup, Czech Championship). The ranking is a single value independent of the event type. When the Ranking facet is active, the ranking SHALL be displayed by its short label and full Czech name on the event detail. When the Ranking facet is not active, the event has no ranking and no ranking section is shown.

#### Scenario: Event detail shows the ranking

- **WHEN** a user views the detail of an event that has the Ranking facet active with a ranking value
- **THEN** the event detail shows the ranking with its short label and full name

#### Scenario: Event detail shows no ranking when the facet is not active

- **WHEN** a user views the detail of an event that does not have the Ranking facet active
- **THEN** the event detail shows no ranking section

### Requirement: Event Base Entry Fee

An event MAY have a **Pricing facet** that carries a base entry fee expressed as a monetary amount with its currency (typically Czech crowns). The base entry fee represents the entry fee of the event's main adult category and is used as the reference price for member contribution calculations. When the Pricing facet is active, the event detail SHALL show the base entry fee amount together with its currency. When the Pricing facet is not active, the event has no base entry fee and no entry fee section SHALL be shown.

#### Scenario: Event detail shows the base entry fee

- **WHEN** a user views the detail of an event that has the Pricing facet active with a base entry fee
- **THEN** the event detail shows the base entry fee amount together with its currency

#### Scenario: Event detail shows no fee when the facet is not active

- **WHEN** a user views the detail of an event that does not have the Pricing facet active
- **THEN** the event detail shows no base entry fee section

### Requirement: ORIS Import Includes Ranking And Base Entry Fee

The system SHALL import an event's ranking and base entry fee from ORIS by activating and filling the Ranking and Pricing facets, both when importing an event for the first time and when synchronizing an already imported event. The ranking is taken from the event's ranking level in ORIS. The base entry fee amount is derived as the highest entry fee among all of the event's categories in ORIS, which corresponds to the main adult category fee and disregards discounted or zero-fee category variants; its currency is taken from the event's currency in ORIS. When ORIS provides no ranking, the Ranking facet is not activated; when ORIS provides no usable category fee, the Pricing facet is not activated.

#### Scenario: Import event with a ranking from ORIS

- **WHEN** an event manager imports an event from ORIS that has a ranking level
- **THEN** the imported event has the Ranking facet active with the ranking set from the ORIS ranking level, including its short label and full name

#### Scenario: Import event with category fees from ORIS

- **WHEN** an event manager imports an event from ORIS whose categories have different entry fees
- **THEN** the imported event has the Pricing facet active with the base entry fee amount set to the highest category fee
- **AND** the base entry fee currency set to the event's currency from ORIS

#### Scenario: Import event without a ranking from ORIS

- **WHEN** an event manager imports an event from ORIS that has no ranking level
- **THEN** the imported event does not have the Ranking facet active

#### Scenario: Import event without usable category fees from ORIS

- **WHEN** an event manager imports an event from ORIS that has no category with a usable entry fee
- **THEN** the imported event does not have the Pricing facet active

#### Scenario: Synchronizing refreshes ranking and fee from ORIS

- **WHEN** an event manager synchronizes an already imported event from ORIS
- **THEN** the Ranking and Pricing facets are activated and updated to the current values from ORIS, or deactivated when ORIS no longer provides them

### Requirement: Manual Editing Of Ranking And Base Entry Fee

An event manager SHALL be able to set or change an event's ranking and base entry fee manually by activating and editing the Ranking and Pricing facets, including for events that were not imported from ORIS. A subsequent synchronization from ORIS overwrites manually entered facet values with the values from ORIS.

#### Scenario: Manager sets ranking and fee on a manually created event

- **WHEN** an event manager activates the Ranking and Pricing facets on a manually created event and sets the ranking and base entry fee
- **THEN** the event detail shows the entered ranking and base entry fee

#### Scenario: Manager corrects an imported event's fee

- **WHEN** an event manager edits the Pricing facet of an imported event and changes its base entry fee
- **THEN** the event detail shows the corrected base entry fee

### Requirement: Event Type Assignment

The system SHALL allow every club event to optionally have an event type assigned from the event types catalog. Events without a type continue to display normally. When a type is set, the event detail page and the events list SHALL display the event type as a colored badge with the type name.

When a manager assigns a type while creating an event, the event's facets SHALL be pre-filled from that type's default facets. The pre-filled set is only a starting point: the manager MAY add or remove facets on the event afterwards, regardless of the type. Changing or clearing the type of an existing event SHALL NOT alter the facets already active on that event.

Users with EVENTS:MANAGE authority SHALL be able to set, change, or clear the event type in the event create form and the event update form.

#### Scenario: Manager creates an event with a type

- **GIVEN** the event types catalog contains "Trénink" and "Pohárový závod"
- **WHEN** a manager fills in the event create form, selects type "Pohárový závod", and submits
- **THEN** the event is created with that type
- **AND** the event detail and event list display "Pohárový závod" as a colored badge

#### Scenario: Selecting a type pre-fills its default facets

- **GIVEN** the event type "Pohárový závod" has default facets Categories, Ranking, and Pricing
- **WHEN** a manager creates an event with type "Pohárový závod"
- **THEN** the new event has the Categories, Ranking, and Pricing facets active

#### Scenario: Manager adjusts facets after the type pre-fill

- **GIVEN** a manager created an event with a type that pre-filled the Categories and Ranking facets
- **WHEN** the manager additionally activates the Pricing facet on that event
- **THEN** the event has Categories, Ranking, and Pricing facets active

#### Scenario: Manager creates an event without a type

- **WHEN** a manager fills in the event create form, leaves the type dropdown empty, and submits
- **THEN** the event is created without a type
- **AND** the event detail and event list show no type badge for the event

#### Scenario: Manager changes the type of an existing event

- **GIVEN** an event has type "Trénink"
- **WHEN** the manager opens the event update form, selects type "Pohárový závod", and submits
- **THEN** the event's type changes to "Pohárový závod"
- **AND** the event's active facets are unchanged

#### Scenario: Manager clears the type of an existing event

- **GIVEN** an event has a type assigned
- **WHEN** the manager opens the update form, clears the type dropdown, and submits
- **THEN** the event has no type
- **AND** the event's active facets are unchanged

### Requirement: Create Event

The system SHALL allow users with EVENTS:MANAGE permission to create events. Required fields: name, event date, organizer code. Optional base fields: location, website URL, coordinator, registration deadline, and an event type. Selecting an event type pre-fills the event's facets from the type's default facets; category, ranking and entry fee data are entered per facet after creation.

#### Scenario: Manager creates an event with all required fields

- **WHEN** user with EVENTS:MANAGE permission submits the event creation form with name, event date, and organizer code
- **THEN** the event is created in DRAFT status
- **AND** appears in the event list

#### Scenario: Manager creates an event with optional base fields

- **WHEN** user with EVENTS:MANAGE permission fills in optional base fields (location, website URL, coordinator, registration deadline) and submits
- **THEN** the event is created with all provided data

#### Scenario: Manager creates an event without a location

- **WHEN** user with EVENTS:MANAGE permission submits the event creation form without a location
- **THEN** the event is created successfully with no location

#### Scenario: Create event button not shown without permission

- **WHEN** user without EVENTS:MANAGE permission views the events list
- **THEN** no create event button is shown

#### Scenario: Form shows validation errors for invalid data

- **WHEN** user submits the event form with missing required fields or invalid formats
- **THEN** the form shows inline validation errors for each issue

#### Scenario: Invalid website URL shows error

- **WHEN** user enters a website URL that is not a valid http/https URL
- **THEN** the form shows an error that the URL must be a valid web address

#### Scenario: Non-existent coordinator shows error

- **WHEN** user references a coordinator member that does not exist
- **THEN** the form shows an error that the coordinator was not found

#### Scenario: Registration deadline after event date shows error

- **WHEN** user sets a registration deadline after the event date
- **THEN** the form shows an error that the deadline must be on or before the event date

### Requirement: Update Event

The system SHALL allow users with EVENTS:MANAGE permission to update an event's base fields in DRAFT or ACTIVE status. Category, ranking and entry fee data are no longer edited here; they are edited through their respective facets.

#### Scenario: Manager updates a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission edits and saves the base fields of a DRAFT event
- **THEN** the event is updated with the new values

#### Scenario: Manager updates an ACTIVE event

- **WHEN** user with EVENTS:MANAGE permission edits and saves the base fields of an ACTIVE event
- **THEN** the event is updated with the new values

#### Scenario: Finished event cannot be edited

- **WHEN** user attempts to edit a FINISHED event
- **THEN** the system shows an error that finished events cannot be modified

#### Scenario: Cancelled event cannot be edited

- **WHEN** user attempts to edit a CANCELLED event
- **THEN** the system shows an error that cancelled events cannot be modified

#### Scenario: Update action not shown without permission

- **WHEN** user without EVENTS:MANAGE permission views an event
- **THEN** no edit action is available

### Requirement: Event Detail Page

The application SHALL display the event detail page with base fields (location and registration deadline when set) and a section for each active facet (categories, ranking, entry fee), and allow managers to edit them inline. Managers SHALL also be able to activate or deactivate facets from the detail page. The registrations section and the link to the registrations list SHALL only be shown for events that are not in DRAFT status.

#### Scenario: Event detail shows location when set

- **WHEN** user views the detail page for an event with a location
- **THEN** the event information section shows the location

#### Scenario: Event detail hides location when not set

- **WHEN** user views the detail page for an event without a location
- **THEN** no location row is shown in the event information section

#### Scenario: Event detail shows registration deadline

- **WHEN** user views the detail page for an event with a registration deadline set
- **THEN** the event information section shows the registration deadline as a formatted date

#### Scenario: Event detail hides registration deadline when not set

- **WHEN** user views the detail page for an event without a registration deadline
- **THEN** no registration deadline row is shown in the event information section

#### Scenario: Inline edit includes registration deadline field

- **WHEN** a manager edits an event inline on the detail page
- **THEN** the registration deadline field is editable as a date picker

#### Scenario: Event create/edit form includes registration deadline

- **WHEN** a manager creates or edits an event via the form
- **THEN** the form includes a registration deadline date picker field

#### Scenario: Event detail shows a section per active facet

- **WHEN** user views the detail page for an event with the Categories and Pricing facets active
- **THEN** the detail page shows a categories section and an entry fee section
- **AND** shows no ranking section

#### Scenario: Manager can manage facets from the detail page

- **WHEN** a manager views the detail page for an event
- **THEN** the manager can activate a facet that is not yet active and deactivate one that is active

#### Scenario: Event detail hides registrations section for DRAFT event

- **WHEN** user views the detail page for an event in DRAFT status
- **THEN** the registrations section and the link to the registrations list are not shown

### Requirement: Event Status Lifecycle

The system SHALL manage event status transitions: DRAFT → ACTIVE → FINISHED or CANCELLED. The transition from ACTIVE to FINISHED is performed exclusively by the automatic completion process; there is no manual "finish" action available to managers. Publishing a DRAFT event (DRAFT → ACTIVE) SHALL be allowed only when every active facet is complete.

When cancelling an event, the manager MAY provide an optional cancellation reason (free text, up to 500 characters). The reason SHALL be stored with the event and SHALL be displayed to viewers of the cancelled event detail; if a reason is set, summary views (e.g. event list) SHALL surface it as supplementary text on the cancelled row.

#### Scenario: Manager publishes a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission publishes an event in DRAFT status whose active facets are all complete
- **THEN** the event becomes ACTIVE
- **AND** members can now register for it

#### Scenario: Publishing is blocked by an incomplete facet

- **GIVEN** a DRAFT event with an active facet that is not fully filled in
- **WHEN** a manager attempts to publish the event
- **THEN** the system prevents the publication and indicates which facet must be completed

#### Scenario: Manager cancels a DRAFT event without a reason

- **WHEN** user with EVENTS:MANAGE permission cancels a DRAFT event and leaves the cancellation reason empty
- **THEN** the event becomes CANCELLED with no reason recorded

#### Scenario: Manager cancels a DRAFT event with a reason

- **WHEN** user with EVENTS:MANAGE permission cancels a DRAFT event and provides a cancellation reason
- **THEN** the event becomes CANCELLED with the reason recorded
- **AND** the cancellation reason is shown on the event detail page

#### Scenario: Manager cancels an ACTIVE event with a reason

- **WHEN** user with EVENTS:MANAGE permission cancels an ACTIVE event and provides a cancellation reason
- **THEN** the event becomes CANCELLED with the reason recorded
- **AND** existing registrations are preserved for records
- **AND** the cancellation reason is shown on the event detail page

#### Scenario: Cancellation reason is shown on the cancelled event row in the list

- **GIVEN** a cancelled event with a recorded cancellation reason
- **WHEN** a user views the event list
- **THEN** the cancelled status indicator on that row exposes the reason as a tooltip or supplementary text

#### Scenario: Invalid status transition shows error

- **WHEN** user attempts an invalid status transition (e.g., FINISHED → ACTIVE)
- **THEN** the system shows an error that the transition is not allowed
