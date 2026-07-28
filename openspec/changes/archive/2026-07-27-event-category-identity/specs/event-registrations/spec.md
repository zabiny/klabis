## ADDED Requirements

### Requirement: Registration Keeps Its Category Across Renames

A registration SHALL stay attached to the category the member selected even when the category is later renamed on the event. When the selected category is removed from the event altogether, the registration SHALL be preserved without a category and presented so that the member or event manager can assign a new one.

#### Scenario: Registration list reflects a renamed category

- **GIVEN** members are registered in a category of an active event
- **WHEN** the event manager renames that category
- **AND** a user opens the registration list for the event
- **THEN** the affected registrations are listed under the new category name

#### Scenario: Member sees the renamed category on their own registration

- **GIVEN** a member is registered in a category that has since been renamed
- **WHEN** the member opens "Moje přihláška" for that event
- **THEN** the new category name is shown

#### Scenario: Registration whose category was removed

- **GIVEN** a member is registered in a category of an active event
- **WHEN** the event manager removes that category from the event
- **AND** a user opens the registration list for the event
- **THEN** the member's registration is still listed
- **AND** the category is shown as not set

#### Scenario: Member re-selects a category after theirs was removed

- **GIVEN** a member's registration has no category because it was removed from the event
- **AND** the registration window is still open
- **WHEN** the member opens the edit form for their registration
- **THEN** the member can select one of the event's current categories
- **AND** submitting the form attaches the registration to the selected category

#### Scenario: Sorting the registration list by category with a removed category

- **GIVEN** a registration list contains registrations whose category was removed
- **WHEN** a user sorts the list by category
- **THEN** registrations without a category are grouped together
- **AND** the remaining registrations are sorted by their category name
