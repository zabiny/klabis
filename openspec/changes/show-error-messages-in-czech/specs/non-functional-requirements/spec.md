## ADDED Requirements

### Requirement: Error Messages Are Shown in Czech

Every error message the application shows to a user SHALL be in Czech. This includes messages caused by rejected business rules, invalid form input, missing records and missing permissions. It does not depend on the language of the user's browser.

#### Scenario: Rejected business rule is explained in Czech

- **WHEN** a user performs an action that the system rejects because a business rule is violated
- **THEN** the user sees a Czech message explaining why the action was rejected

#### Scenario: Invalid form input is explained in Czech

- **WHEN** a user submits a form with a value the system does not accept
- **THEN** the message describing the problem is in Czech

#### Scenario: Browser language does not change the message language

- **WHEN** a user whose browser prefers English performs an action that the system rejects
- **THEN** the message the user sees is in Czech

### Requirement: Error Messages Refer to Things by Their Names

An error message SHALL refer to members, events, event types, groups, fee levels and other records by the names the user knows from the application. It SHALL NOT show internal identifiers, technical field keys or technical exception texts.

#### Scenario: Message names the record instead of its identifier

- **WHEN** the system rejects an action because of a conflict with an existing record
- **THEN** the message names that record the way it is shown elsewhere in the application
- **AND** the message contains no internal identifier

#### Scenario: Message does not show technical field names

- **WHEN** the system rejects a form because of several invalid values
- **THEN** each problem is described with the field label the user sees in the form, not with a technical field key

### Requirement: Form Errors Are Shown Next to the Related Field

When the system rejects a submitted form because of a problem with a specific field, the message SHALL be shown next to that field, and the form SHALL stay open with the entered values. Problems that are not tied to a single field SHALL be shown as one message above the form.

#### Scenario: Field problem reported by the system appears at the field

- **WHEN** a user submits a form and the system rejects the value of one field
- **THEN** the Czech message appears next to that field
- **AND** the values the user entered remain in the form

#### Scenario: Problem of the whole form appears above the form

- **WHEN** a user submits a form and the system rejects it for a reason not tied to one field
- **THEN** a Czech message explaining the reason appears above the form
- **AND** the values the user entered remain in the form

### Requirement: Unexpected Errors Show a Generic Czech Message

When the system cannot give a specific explanation for a failure, the user SHALL see a generic Czech message saying that the action failed and can be retried. Technical details of the failure SHALL NOT be shown to the user.

#### Scenario: Unexpected failure shows a generic message

- **WHEN** a user performs an action that fails for an unexpected reason
- **THEN** the user sees a generic Czech message that the action failed and can be retried
- **AND** no technical error text is shown
