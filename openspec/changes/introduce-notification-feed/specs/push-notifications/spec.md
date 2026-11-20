# Spec Delta

## ADDED Requirements

### Requirement: Notification Devices Are Managed On The Notification Settings Tab

The push device management (enabling and disabling notifications on the current device, the device list and the test notification) SHALL be part of the "Nastavení notifikací" tab of the "Nastavení" page. When the server is not configured to send push notifications, the tab SHALL still be shown, without the device part.

#### Scenario: User opens device management

- **WHEN** a logged-in user opens "Nastavení" in the main menu and selects the "Nastavení notifikací" tab
- **THEN** the tab shows the device management together with the notification settings table

#### Scenario: Push is not configured on the server

- **GIVEN** the server is not configured to send push notifications
- **WHEN** a logged-in user opens the "Nastavení notifikací" tab
- **THEN** the notification settings table is shown
- **AND** no device management is shown

## MODIFIED Requirements

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
- **THEN** Klabis opens on the "Nastavení notifikací" tab

## REMOVED Requirements

### Requirement: Notification Settings Are Reachable From The Home Page

**Reason**: The device settings move to the "Nastavení notifikací" tab under the new "Nastavení" main-menu entry, which is shown whether or not push is configured on the server. The bell in the top bar leads to the notification feed.

**Migration**: Users open "Nastavení" → "Nastavení notifikací" from the main menu, or follow the settings link on the "Notifikace" feed page. The home-page "Notifikace" panel is removed.
