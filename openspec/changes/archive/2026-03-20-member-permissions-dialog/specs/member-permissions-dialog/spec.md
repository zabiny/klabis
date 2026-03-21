## ADDED Requirements

### Requirement: Open permissions dialog from member detail

The system SHALL allow ADMIN users to manage member permissions via a modal dialog accessible from the member detail page.

#### Scenario: Dialog opens when permissions link is present

- **WHEN** ADMIN user views member detail page
- **AND** member response includes `permissions` HATEOAS link
- **THEN** "Správa oprávnění" button is displayed
- **AND** clicking the button opens a modal dialog without navigating away from the page

#### Scenario: Dialog is not shown without permissions link

- **WHEN** authenticated user views member detail page
- **AND** member response does NOT include `permissions` HATEOAS link
- **THEN** "Správa oprávnění" button is not displayed

### Requirement: Display current permissions in dialog

The system SHALL load and display the user's current permissions when the dialog opens.

#### Scenario: Permissions loaded and displayed as toggles

- **WHEN** dialog opens
- **THEN** system fetches current permissions via GET on the URL from `permissions` HATEOAS link
- **AND** dialog displays all available authorities as toggle switches
- **AND** authorities currently assigned to the user are shown as enabled (on)
- **AND** authorities not assigned to the user are shown as disabled (off)
- **AND** each authority is displayed with a Czech label and description

#### Scenario: Loading state while fetching permissions

- **WHEN** dialog opens and permissions are being fetched
- **THEN** dialog displays a loading indicator
- **AND** "Uložit oprávnění" button is disabled until data is loaded

### Requirement: Save updated permissions

The system SHALL allow ADMIN user to modify toggles and save the updated permission set.

#### Scenario: Permissions saved successfully

- **WHEN** ADMIN user changes toggle states in the dialog
- **AND** clicks "Uložit oprávnění"
- **THEN** system sends PUT request with updated authority list to the URL from HATEOAS affordance
- **AND** dialog closes
- **AND** success toast notification "Oprávnění uložena" is displayed

#### Scenario: Save fails with admin lockout error

- **WHEN** ADMIN user attempts to remove MEMBERS:PERMISSIONS from the last admin
- **AND** clicks "Uložit oprávnění"
- **THEN** system receives 409 Conflict from the API
- **AND** dialog remains open
- **AND** error message is displayed inside the dialog explaining the operation is not allowed

#### Scenario: Save fails with other error

- **WHEN** PUT request fails with any non-409 error
- **THEN** dialog remains open
- **AND** error message is displayed inside the dialog

#### Scenario: Cancel dialog without saving

- **WHEN** ADMIN user clicks "Zrušit"
- **THEN** dialog closes without sending any API request
- **AND** no changes are applied
