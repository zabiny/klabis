## MODIFIED Requirements

### Requirement: Display Current Permissions in Dialog

The system SHALL load and display the user's current permissions when the dialog opens. The dialog SHALL include all available authorities, including `GROUPS:TRAINING`.

#### Scenario: Dialog shows current permissions as toggles

- **WHEN** user opens the permissions dialog
- **THEN** the dialog displays all available authorities as toggle switches
- **AND** authorities currently assigned to the user are shown as enabled
- **AND** authorities not assigned are shown as disabled
- **AND** each authority is displayed with a Czech label and description

#### Scenario: Dialog shows GROUPS:TRAINING permission toggle

- **WHEN** user opens the permissions dialog
- **THEN** the dialog displays a toggle for `GROUPS:TRAINING` with the label "Správa tréninkových skupin"
- **AND** the toggle reflects whether the member currently has `GROUPS:TRAINING` assigned

#### Scenario: Dialog shows loading state while fetching permissions

- **WHEN** dialog opens and permissions are being fetched
- **THEN** a loading indicator is shown
- **AND** the "Uložit oprávnění" button is disabled until data is loaded
