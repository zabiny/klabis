## ADDED Requirements

### Requirement: Removing The Viewed Resource Returns To Its List

When a user's action removes the resource they are viewing, or removes their own access to it, the system SHALL take them to the list that resource belongs to and show the success message, instead of leaving them on a page that can no longer be shown.

#### Scenario: Owner deletes a free group from its detail page

- **WHEN** a free group owner confirms "Smazat skupinu" on the group detail page
- **THEN** the system shows a success message
- **AND** the user is taken to the group management page
- **AND** the deleted group is not listed there

#### Scenario: Member leaves a free group from its detail page

- **WHEN** a free group member who is not an owner confirms "Opustit skupinu" on the group detail page
- **THEN** the system shows a success message
- **AND** the user is taken to the group management page
- **AND** no access denied error is shown

#### Scenario: Owner removes themself from the owners of a free group

- **WHEN** a free group owner removes themself from the group's owners and confirms
- **THEN** the system shows a success message
- **AND** the user is taken to the group management page

### Requirement: Success Handling Of A Form Action Is Never Lost

After a successful form submission the system SHALL always complete its success handling — show the success message, close the dialog and navigate where the action leads — even when the refreshed page no longer offers the action or the resource it acted on.

#### Scenario: Member unregisters from an event in the registration dialog

- **WHEN** a member chooses "Odhlásit" in their registration dialog on the event detail page and confirms
- **THEN** the system shows a success message
- **AND** the dialog closes
- **AND** the event detail page no longer lists the member among the registered

## MODIFIED Requirements

### Requirement: Successful Create Submissions Navigate to the New Resource

The system SHALL navigate the user to the newly created resource immediately after a successful create-style form submission. A create-style submission is any form submission that results in a `201 Created` response carrying a `Location` header that points at the new resource.

Edit submissions (`200 OK` / `204 No Content` without a `Location` header) and delete submissions SHALL NOT navigate the user elsewhere; they continue to refresh the current page. The exception is a submission that removes the resource the user is viewing, or the user's access to it, which returns the user to that resource's list (see "Removing The Viewed Resource Returns To Its List").

#### Scenario: Manager creates a family group and lands on its detail page

- **WHEN** a user with MEMBERS:MANAGE permission submits the "create family group" form successfully
- **THEN** the system shows a success toast
- **AND** the user is navigated to the detail page of the newly created family group
- **AND** the family groups list the user came from is no longer visible

#### Scenario: Manager creates a training group and lands on its detail page

- **WHEN** a user with GROUPS:TRAINING permission submits the "create training group" form successfully
- **THEN** the user is navigated to the detail page of the newly created training group

#### Scenario: Member creates a free group and lands on its detail page

- **WHEN** an authenticated member submits the "create free group" form successfully
- **THEN** the user is navigated to the detail page of the newly created free group

#### Scenario: Manager edits an existing event and stays on the current page

- **WHEN** a user with EVENTS:MANAGE permission submits an edit form for an existing event
- **THEN** the current page is refreshed with the updated values
- **AND** the user is not navigated elsewhere

#### Scenario: Manager deletes a category preset and stays on the current page

- **WHEN** a user with EVENTS:MANAGE permission confirms deletion of a category preset
- **THEN** the preset is removed from the list
- **AND** the user stays on the category presets list page

#### Scenario: Successful create submission without a Location header does not navigate

- **WHEN** a create-style form submission succeeds but the backend response does not include a `Location` header
- **THEN** the user is not navigated elsewhere
- **AND** the current page is refreshed
