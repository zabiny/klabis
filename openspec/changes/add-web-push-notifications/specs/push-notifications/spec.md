# Spec Delta

## Purpose

Lets users receive Klabis notifications as native operating-system notifications on their phones and computers, even when the app is closed. Covers enabling notifications per device, managing the devices that receive them, testing delivery, and what happens when a notification is clicked.

## ADDED Requirements

### Requirement: Notification Settings Are Reachable From The Home Page

The system SHALL offer a "Notifikace" entry on the home page to every logged-in user when push notifications are available on the server. The entry opens the notification settings page.

#### Scenario: User opens notification settings

- **WHEN** a logged-in user clicks "Notifikace" on the home page
- **THEN** the notification settings page opens

#### Scenario: Notifications are not configured on the server

- **GIVEN** the server is not configured to send push notifications
- **WHEN** a logged-in user opens the home page
- **THEN** no "Notifikace" entry is shown

### Requirement: Enabling Notifications On The Current Device

The notification settings page SHALL let the user enable notifications on the device and browser they are currently using. The operating system's permission prompt SHALL be shown only after the user clicks "Zapnout notifikace na tomto zařízení", never automatically.

#### Scenario: User enables notifications on a device

- **GIVEN** notifications are not enabled on the current device
- **WHEN** the user clicks "Zapnout notifikace na tomto zařízení" and allows notifications in the operating system prompt
- **THEN** the page shows that notifications are enabled on this device
- **AND** the device appears in the list of the user's notification devices

#### Scenario: User declines the operating system prompt

- **WHEN** the user clicks "Zapnout notifikace na tomto zařízení" and blocks notifications in the operating system prompt
- **THEN** the page shows that notifications are blocked in the browser and explains how to allow them in the browser settings
- **AND** the device is not added to the list of notification devices

#### Scenario: Opening the page does not prompt for permission

- **WHEN** the user opens the notification settings page
- **THEN** no operating system permission prompt is shown

### Requirement: Disabling Notifications On The Current Device

The notification settings page SHALL let the user disable notifications on the current device. Afterwards the device SHALL receive no further Klabis notifications.

#### Scenario: User disables notifications on the current device

- **GIVEN** notifications are enabled on the current device
- **WHEN** the user clicks "Vypnout notifikace na tomto zařízení"
- **THEN** the page shows that notifications are disabled on this device
- **AND** the device disappears from the list of notification devices

### Requirement: Notifications Unavailable On The Current Device

When notifications cannot be enabled on the current device, the notification settings page SHALL explain why instead of offering the enable action. The device list and the test notification SHALL stay available.

#### Scenario: Browser does not support notifications

- **WHEN** the user opens the notification settings page in a browser that does not support push notifications
- **THEN** the page states that this browser cannot receive notifications
- **AND** the "Zapnout notifikace na tomto zařízení" button is not shown

#### Scenario: iPhone or iPad without the app installed

- **WHEN** the user opens the notification settings page in Safari on an iPhone or iPad without having added Klabis to the home screen
- **THEN** the page explains that notifications require adding Klabis to the home screen first, and how to do it

#### Scenario: Notifications previously blocked

- **GIVEN** the user has blocked notifications for Klabis in the browser
- **WHEN** the user opens the notification settings page
- **THEN** the page states that notifications are blocked and explains how to allow them in the browser settings

### Requirement: Managing Notification Devices

The notification settings page SHALL list every device on which the user has enabled notifications, with a recognizable device description (browser and operating system) and the date it was enabled. The current device SHALL be marked. The user SHALL be able to remove any of their devices. A user SHALL see and manage only their own devices.

#### Scenario: User sees their devices

- **GIVEN** the user has enabled notifications on a phone and on a laptop
- **WHEN** the user opens the notification settings page on the laptop
- **THEN** both devices are listed with their description and enable date
- **AND** the laptop is marked as the current device

#### Scenario: User removes another device

- **GIVEN** the user has enabled notifications on a phone they no longer use
- **WHEN** the user clicks "Odebrat" next to that phone in the device list
- **THEN** the phone disappears from the list
- **AND** the phone receives no further Klabis notifications

#### Scenario: Devices of other users are not visible

- **WHEN** a user opens the notification settings page
- **THEN** only devices on which that user enabled notifications are listed

### Requirement: Test Notification

The notification settings page SHALL offer "Poslat testovací notifikaci" whenever the user has at least one notification device. It SHALL send a test notification to all of the user's devices.

#### Scenario: User sends a test notification

- **GIVEN** the user has notifications enabled on at least one device
- **WHEN** the user clicks "Poslat testovací notifikaci"
- **THEN** each of the user's devices shows an operating system notification from Klabis confirming that notifications work
- **AND** the page confirms that the test notification was sent

#### Scenario: No devices registered

- **GIVEN** the user has no notification devices
- **WHEN** the user opens the notification settings page
- **THEN** the "Poslat testovací notifikaci" button is not shown

### Requirement: Notifications Arrive While The App Is Closed

A notification sent to a user SHALL be shown by the operating system on each of the user's notification devices, whether or not Klabis is currently open there. Each notification SHALL show the Klabis icon, a title and a short text.

#### Scenario: Notification on a phone with the app closed

- **GIVEN** the user enabled notifications on their phone and Klabis is closed
- **WHEN** a notification is sent to the user
- **THEN** the phone shows the notification with the Klabis icon, its title and text

### Requirement: Clicking A Notification Opens The Related Page

Clicking a notification SHALL bring Klabis to the foreground on the page the notification refers to, reusing an already open Klabis window when there is one. A notification without a specific page SHALL open the home page.

#### Scenario: Click with Klabis closed

- **GIVEN** Klabis is not open on the device
- **WHEN** the user clicks a notification that refers to an event
- **THEN** Klabis opens on that event's detail page

#### Scenario: Click with Klabis already open

- **GIVEN** Klabis is already open on the device
- **WHEN** the user clicks a notification
- **THEN** the open Klabis window comes to the foreground and shows the page the notification refers to
- **AND** no second Klabis window is opened

#### Scenario: Click on the test notification

- **WHEN** the user clicks the test notification
- **THEN** Klabis opens on the notification settings page

### Requirement: Logging Out Stops Notifications On That Device

When the user explicitly logs out with "Odhlásit", the system SHALL stop sending notifications to that device. The user's other devices SHALL keep receiving notifications.

#### Scenario: User logs out on a shared computer

- **GIVEN** the user has notifications enabled on a shared computer and on their phone
- **WHEN** the user clicks "Odhlásit" on the shared computer
- **THEN** the shared computer receives no further notifications for that user
- **AND** the phone keeps receiving notifications

### Requirement: Unreachable Devices Are Removed Automatically

When a device can no longer receive notifications, for example because notifications were revoked in the browser or the browser data was cleared, the system SHALL remove it from the user's device list on the next delivery attempt.

#### Scenario: Notifications revoked in the browser

- **GIVEN** the user had notifications enabled on a laptop and later revoked the notification permission in the browser
- **WHEN** a notification is sent to the user
- **THEN** the laptop disappears from the user's list of notification devices

### Requirement: Notification Content Is Privacy-Safe

Notifications SHALL NOT contain sensitive personal data (birth numbers, bank accounts, account balances, health or contact data of other members), because they can appear on a locked screen.

#### Scenario: Notification on a locked screen

- **WHEN** a notification is displayed on a locked phone
- **THEN** it contains only a title and short text without sensitive personal data
