## MODIFIED Requirements

### Requirement: Open Permissions Dialog from Member Detail

The system SHALL allow users with MEMBERS:PERMISSIONS authority to manage member permissions via a modal dialog accessible from the member detail page.

#### Scenario: Permissions button shown when permissions link is available

- **WHEN** user with MEMBERS:PERMISSIONS authority views a member detail page
- **AND** the member response includes a permissions link
- **THEN** the "Správa oprávnění" button is displayed in the page header

#### Scenario: Permissions button not shown without the permissions link

- **WHEN** user views a member detail page
- **AND** the member response does not include a permissions link
- **THEN** the "Správa oprávnění" button is not displayed

### Requirement: Display Current Permissions in Dialog

The system SHALL load and display the user's current permissions when the dialog opens.

#### Scenario: Dialog shows current permissions as toggles

- **WHEN** user opens the permissions dialog
- **THEN** the dialog displays all available authorities as toggle switches
- **AND** authorities currently assigned to the user are shown as enabled
- **AND** authorities not assigned are shown as disabled
- **AND** each authority is displayed with a Czech label and description

#### Scenario: Dialog shows loading state while fetching permissions

- **WHEN** dialog opens and permissions are being fetched
- **THEN** a loading indicator is shown
- **AND** the "Uložit oprávnění" button is disabled until data is loaded

### Requirement: Save Updated Permissions

The system SHALL allow user to modify toggles and save the updated permission set.

#### Scenario: Permissions saved successfully

- **WHEN** user changes toggle states in the dialog and clicks "Uložit oprávnění"
- **THEN** the updated permission set is saved
- **AND** the dialog closes
- **AND** a success notification "Oprávnění uložena" is shown

#### Scenario: Save fails because last admin would be locked out

- **WHEN** user attempts to remove MEMBERS:PERMISSIONS from the last admin
- **AND** clicks "Uložit oprávnění"
- **THEN** the dialog remains open
- **AND** an error message is shown inside the dialog explaining the operation is not allowed

#### Scenario: Save fails with unexpected error

- **WHEN** saving permissions fails with an unexpected error
- **THEN** the dialog remains open
- **AND** an error message is shown inside the dialog

#### Scenario: Cancel dialog without saving

- **WHEN** user clicks "Zrušit" in the dialog
- **THEN** the dialog closes without saving any changes
