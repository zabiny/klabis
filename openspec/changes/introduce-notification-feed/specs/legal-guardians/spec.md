# Spec Delta

## MODIFIED Requirements

### Requirement: Notifications About A Minor Reach Their Guardians

Any notification the system sends about a minor member SHALL appear in the feed of the minor and of every legal guardian. E-mail, when switched on in the minor's notification settings, SHALL be sent to all of the minor's legal guardians and, if the minor has an own e-mail, also to the minor. Push, when switched on in the minor's notification settings, SHALL be sent only to the minor. Notifications about an adult member SHALL reach only the member, by the channels switched on in the member's own settings.

#### Scenario: Notification about a minor with two guardians

- **GIVEN** a minor with an own e-mail and two legal guardians, with e-mail switched on in the minor's settings
- **WHEN** the system sends a notification about the minor
- **THEN** the notification appears in the feed of the minor and of both guardians
- **AND** an e-mail is sent to both guardians and to the minor

#### Scenario: Notification about a minor without an own e-mail

- **GIVEN** a minor without an own e-mail and one legal guardian, with e-mail switched on in the minor's settings
- **WHEN** the system sends a notification about the minor
- **THEN** an e-mail is sent only to the guardian

#### Scenario: Push about a minor goes only to the minor

- **GIVEN** a minor with push switched on in their settings
- **WHEN** the system sends a notification about the minor
- **THEN** only the minor's devices show a push notification

#### Scenario: Notification about an adult

- **WHEN** the system sends a notification about an adult member
- **THEN** the notification reaches only the member, in their feed and by the channels switched on in their settings
