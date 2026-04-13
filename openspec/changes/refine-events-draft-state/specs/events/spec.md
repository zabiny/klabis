## MODIFIED Requirements

### Requirement: Event Detail Page

The application SHALL display the event detail page with registration deadline (when set) and categories (when defined), and allow managers to edit them inline. The registrations section and the link to the registrations list SHALL only be shown for events that are not in DRAFT status.

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

#### Scenario: Event detail shows categories

- **WHEN** user views the detail page for an event with categories defined
- **THEN** the categories are displayed as individual pills/tags

#### Scenario: Event detail hides categories when not set

- **WHEN** user views the detail page for an event without categories
- **THEN** no categories row is shown

#### Scenario: Inline edit includes categories field

- **WHEN** a manager edits an event inline on the detail page
- **THEN** the categories field is editable

#### Scenario: Event detail hides registrations section for DRAFT event

- **WHEN** user views the detail page for an event in DRAFT status
- **THEN** the registrations section is NOT shown
- **AND** no link to the registrations list is displayed

#### Scenario: Event detail shows registrations section for ACTIVE event

- **WHEN** user views the detail page for an event in ACTIVE status
- **THEN** the registrations section is shown with the link to the registrations list

## ADDED Requirements

### Requirement: Apply Category Preset in Event Form

The system SHALL allow event managers to populate the categories field in the event create/edit form by selecting an existing Category Preset. Selecting a preset replaces the current categories field value with the preset's categories. The manager can further edit the categories manually after applying a preset.

#### Scenario: Manager applies a category preset in the event form

- **WHEN** a manager opens the event create or edit form
- **AND** at least one Category Preset exists
- **AND** the manager clicks the "Select from templates" button next to the categories field
- **AND** selects a preset from the list
- **THEN** the categories field is populated with the categories from the selected preset
- **AND** the manager can still edit the categories manually before saving

#### Scenario: Category preset picker is not shown when no presets exist

- **WHEN** a manager opens the event create or edit form
- **AND** no Category Presets exist in the system
- **THEN** the "Select from templates" button is NOT shown next to the categories field
