# Spec Delta

## Purpose

Keeps every user informed about things in the club that concern them: an in-app feed of their notifications, a choice of which notification types also reach them by push or e-mail, and routing of notifications about a minor to the minor and their legal guardians.

## ADDED Requirements

### Requirement: Notification Bell Shows Unread Notifications

The top bar SHALL show a bell to every logged-in user. The bell SHALL show the number of the user's unread notifications, or no number when there are none. Clicking the bell SHALL open the "Notifikace" page.

#### Scenario: User has unread notifications

- **GIVEN** the user has three unread notifications
- **WHEN** the user looks at the top bar
- **THEN** the bell shows the number 3

#### Scenario: User has no unread notifications

- **GIVEN** all of the user's notifications are read
- **WHEN** the user looks at the top bar
- **THEN** the bell shows no number

#### Scenario: User opens the feed from the bell

- **WHEN** the user clicks the bell
- **THEN** the "Notifikace" page opens

#### Scenario: Many unread notifications

- **GIVEN** the user has 120 unread notifications
- **WHEN** the user looks at the top bar
- **THEN** the bell shows "99+"

### Requirement: Notification Feed Lists The User's Notifications

The "Notifikace" page SHALL list the notifications addressed to the current user, newest first. Each notification SHALL show its title, its text and when it was created. Unread notifications SHALL be visually distinguished from read ones. A user SHALL see only notifications addressed to them.

#### Scenario: User sees their notifications

- **GIVEN** the user received a notification yesterday and another one today
- **WHEN** the user opens the "Notifikace" page
- **THEN** both notifications are listed with title, text and creation time
- **AND** today's notification is listed first

#### Scenario: Unread notification is highlighted

- **GIVEN** the user has one read and one unread notification
- **WHEN** the user opens the "Notifikace" page
- **THEN** the unread notification is visually distinguished from the read one

#### Scenario: Notifications of other users are not visible

- **GIVEN** a notification was sent to another member
- **WHEN** the user opens the "Notifikace" page
- **THEN** that notification is not listed

#### Scenario: Empty feed

- **GIVEN** the user has no notifications
- **WHEN** the user opens the "Notifikace" page
- **THEN** the page states that there are no notifications

### Requirement: Opening A Notification Marks It Read

Clicking a notification in the feed SHALL mark it read and open the page the notification refers to. A notification without a specific page SHALL only be marked read.

#### Scenario: User opens an unread notification

- **GIVEN** the user has an unread notification about their financial account
- **WHEN** the user clicks the notification on the "Notifikace" page
- **THEN** the "Finance" page with the user's account opens
- **AND** the notification is shown as read when the user returns to the feed
- **AND** the number on the bell decreases by one

### Requirement: Marking All Notifications Read

The "Notifikace" page SHALL offer "Označit vše jako přečtené" while the user has unread notifications. It SHALL mark all of the user's notifications read.

#### Scenario: User marks all notifications read

- **GIVEN** the user has five unread notifications
- **WHEN** the user clicks "Označit vše jako přečtené"
- **THEN** all notifications in the feed are shown as read
- **AND** the bell shows no number

#### Scenario: Nothing to mark

- **GIVEN** all of the user's notifications are read
- **WHEN** the user opens the "Notifikace" page
- **THEN** "Označit vše jako přečtené" is not offered

### Requirement: Notifications Expire After 90 Days

Notifications SHALL disappear from the feed 90 days after they were created, whether they were read or not. The user SHALL NOT be able to delete individual notifications.

#### Scenario: Old notification disappears

- **GIVEN** the user received a notification 91 days ago
- **WHEN** the user opens the "Notifikace" page
- **THEN** that notification is not listed

#### Scenario: Notification cannot be deleted

- **WHEN** the user views a notification on the "Notifikace" page
- **THEN** no action to delete the notification is offered

### Requirement: Feed Links To Notification Settings

The "Notifikace" page SHALL offer a link to the "Nastavení notifikací" tab.

#### Scenario: User opens settings from the feed

- **WHEN** the user follows the settings link on the "Notifikace" page
- **THEN** the "Nastavení notifikací" tab of the "Nastavení" page opens

### Requirement: Feed Receives Every Notification

Every notification addressed to a user SHALL appear in the user's feed, regardless of the user's notification settings. The settings SHALL govern only delivery by push and e-mail.

#### Scenario: Notification with push and e-mail switched off

- **GIVEN** the user has switched off both push and e-mail for negative balance notifications
- **WHEN** the user's balance drops below zero
- **THEN** the user receives no push notification and no e-mail
- **AND** the notification appears in the user's feed

### Requirement: Notification Settings Per Type And Channel

The "Nastavení notifikací" tab SHALL show a table with one row per notification type and one column per delivery channel, push and e-mail. The user SHALL be able to switch each channel on or off for each type. The settings SHALL apply to all of the user's devices and SHALL take effect for the next notification.

#### Scenario: User switches off e-mail for a type

- **GIVEN** e-mail is on for negative balance notifications
- **WHEN** the user switches off e-mail in the "Záporný zůstatek" row and saves
- **THEN** the next negative balance notification reaches the user in the feed and by push, but not by e-mail

#### Scenario: Settings are shown even without push devices

- **GIVEN** the user has no notification device
- **WHEN** the user opens the "Nastavení notifikací" tab
- **THEN** the table of notification types and channels is shown

### Requirement: Default Notification Settings

Until a user changes a setting, each notification type SHALL use its default per channel, the same for adults and minors. For the negative balance notification the defaults SHALL be: e-mail on, push on.

#### Scenario: New adult member

- **GIVEN** an adult member who never changed their notification settings
- **WHEN** the member opens the "Nastavení notifikací" tab
- **THEN** push and e-mail are shown switched on in the "Záporný zůstatek" row

#### Scenario: New minor member

- **GIVEN** a minor member whose settings were never changed
- **WHEN** a legal guardian views the minor's notification settings
- **THEN** push and e-mail are shown switched on in the "Záporný zůstatek" row

### Requirement: Push Delivery Follows The Settings

When a notification is created for a user and the user has push switched on for its type, the system SHALL send it as a push notification to all of the user's notification devices. A push notification SHALL never contain sensitive personal data such as an account balance.

#### Scenario: Push is on

- **GIVEN** the user has push switched on for negative balance notifications and has a phone with notifications enabled
- **WHEN** the user's balance drops below zero
- **THEN** the phone shows a notification that the balance is negative, without the amount

#### Scenario: Push is off

- **GIVEN** the user has push switched off for negative balance notifications
- **WHEN** the user's balance drops below zero
- **THEN** none of the user's devices shows a notification

### Requirement: E-mail Delivery Follows The Settings

When a notification is created and e-mail is switched on for its type, the system SHALL send it by e-mail immediately after the change that caused it is saved. An e-mail SHALL carry the notification's title and text and a link to the page it refers to. A recipient without an e-mail address SHALL be skipped. A failed e-mail SHALL NOT undo the change that caused it.

#### Scenario: E-mail is on

- **GIVEN** the member has e-mail switched on for negative balance notifications
- **WHEN** the member's balance drops below zero
- **THEN** the member receives an e-mail stating the new balance with a link to their financial account

#### Scenario: Member without an e-mail address

- **GIVEN** a member without an e-mail address and with e-mail switched on
- **WHEN** a notification is created for the member
- **THEN** no e-mail is sent
- **AND** the notification still appears in the member's feed

#### Scenario: Mail server is unavailable

- **GIVEN** the mail server is unavailable
- **WHEN** a finance manager records a charge that drops a member's balance below zero
- **THEN** the charge is recorded
- **AND** the notification appears in the member's feed

### Requirement: Notifications About A Minor Are Routed To Their Guardians

A notification about a minor SHALL appear in the feed of the minor and of every legal guardian of the minor, including guardians who are not members. E-mail, when switched on in the minor's settings, SHALL go to every legal guardian and to the minor's own e-mail if the minor has one. Push, when switched on in the minor's settings, SHALL go to the devices of every legal guardian and never to the minor's devices.

#### Scenario: Minor with two guardians and push off

- **GIVEN** a minor with an own e-mail, two legal guardians, e-mail on and push off in the minor's settings
- **WHEN** a notification about the minor is created
- **THEN** the notification appears in the feed of the minor and of both guardians
- **AND** an e-mail is sent to both guardians and to the minor
- **AND** no push notification is sent

#### Scenario: Minor with push on

- **GIVEN** a minor whose settings have push switched on, two legal guardians with notifications enabled on their phones, and a minor with notifications enabled on their own phone
- **WHEN** a notification about the minor is created
- **THEN** both guardians' phones show the notification
- **AND** the minor's phone shows no push notification for it

#### Scenario: Guardian's own settings do not apply to their child's notifications

- **GIVEN** a guardian who switched off e-mail for negative balance notifications in their own settings
- **AND** their minor child has e-mail switched on
- **WHEN** the child's balance drops below zero
- **THEN** the guardian receives an e-mail about the child's negative balance

#### Scenario: Guardian sees which child the notification is about

- **WHEN** a guardian opens a notification about their minor child in the feed
- **THEN** the notification names the child, in the same wording the child sees
- **AND** clicking it opens the child's detail page

### Requirement: Guardians Manage A Minor's Notification Settings

Each legal guardian SHALL see, on their own "Nastavení notifikací" tab, a separate section for each of their minor children with the child's notification settings, and SHALL be able to change them. A minor SHALL see their own notification settings read-only, with a note that their legal guardians manage them. Once the member turns 18, the member SHALL manage their settings and the former guardians' section for them SHALL disappear.

#### Scenario: Guardian changes a child's settings

- **GIVEN** a guardian of two minor children
- **WHEN** the guardian opens the "Nastavení notifikací" tab
- **THEN** a section for each child is shown with the child's notification settings
- **AND** the guardian can switch e-mail off for one child without affecting the other

#### Scenario: Minor views their settings

- **WHEN** a minor opens the "Nastavení notifikací" tab
- **THEN** their notification settings are shown read-only
- **AND** a note says that their legal guardians manage them

#### Scenario: Member turns 18

- **GIVEN** a minor whose guardians switched e-mail off in their settings
- **WHEN** the member turns 18 and is no longer a minor
- **THEN** the member can change their notification settings, starting from the values the guardians set
- **AND** push notifications now reach the member's own devices instead of the former guardians'
- **AND** the former guardians no longer see a section for the member

#### Scenario: Unrelated user cannot change a member's settings

- **WHEN** a member who is neither the minor nor one of their legal guardians tries to change the minor's notification settings
- **THEN** the change is refused

### Requirement: Suspended Members Keep Receiving Notifications

A member whose membership is suspended SHALL keep receiving notifications in the feed and over the channels switched on in their settings.

#### Scenario: Suspended member with a negative balance

- **GIVEN** a member whose membership is suspended
- **WHEN** a finance manager records a charge that drops the member's balance below zero
- **THEN** the member receives the negative balance notification as any other member would
