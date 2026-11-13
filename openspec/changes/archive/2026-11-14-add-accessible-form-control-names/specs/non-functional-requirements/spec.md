## ADDED Requirements

### Requirement: Form Fields Announced by Their Visible Label

Every form field in the application SHALL be identifiable by assistive technology (screen readers, voice control) through the same text that is shown to sighted users as the field's label. This applies to fields in dialogs as well as to fields edited inline on detail pages, where the label is shown beside the field rather than above it.

#### Scenario: Screen-reader user edits an event inline

- **GIVEN** an event manager uses a screen reader and opens the detail page of a draft event
- **WHEN** the manager switches the page to edit mode and moves focus to the field shown next to the label "Místo"
- **THEN** the screen reader announces the field as "Místo" together with its type and current value

#### Scenario: Selection field announces its label

- **GIVEN** a user with a screen reader edits a form that contains a selection list labelled "Stupeň"
- **WHEN** the user moves focus to the selection list
- **THEN** the screen reader announces "Stupeň" and the currently selected option

#### Scenario: Group of options announces its label

- **GIVEN** a user with a screen reader edits a form that contains a group of checkboxes or radio buttons under a visible label
- **WHEN** the user moves focus into the group
- **THEN** the screen reader announces the group's visible label before the individual options

#### Scenario: Voice-control user activates a field by its label

- **GIVEN** a user operates the application with voice control on a page showing a field labelled "Datum konání"
- **WHEN** the user says "click Datum konání"
- **THEN** focus moves to that field

### Requirement: Action Buttons Announced by Their Visible Text

Every action button SHALL be announced by assistive technology with the same Czech text that is displayed on the button. A button that shows only an icon SHALL still be announced with a descriptive Czech name of the action it performs.

#### Scenario: Screen-reader user hears the visible button text

- **GIVEN** an administrator uses a screen reader on the member list page
- **WHEN** the administrator moves focus to the "Registrovat člena" button
- **THEN** the screen reader announces "Registrovat člena, button"
- **AND** no English or otherwise different wording is announced instead of the visible text

#### Scenario: Icon-only action button has a descriptive name

- **GIVEN** a user with a screen reader views a page where an action is offered as an icon-only button
- **WHEN** the user moves focus to that button
- **THEN** the screen reader announces the Czech name of the action (the same text that would be shown on a labelled button)

#### Scenario: Voice-control user activates an action by its visible text

- **GIVEN** an event manager operates the application with voice control on the detail page of a draft event
- **WHEN** the manager says "click Publikovat"
- **THEN** the "Publikovat" action is triggered
