## MODIFIED Requirements

### Requirement: Upcoming Deadlines Widget

The home dashboard SHALL show a "Končící přihlášky tento týden" widget that lists active events (status ACTIVE) whose nearest future registration deadline falls within the next seven days (today through today+7), and to which the current user is NOT yet registered. The widget appears only for users with a member profile.

The widget SHALL list at most five events, ordered by registration deadline ascending (the soonest-closing first). Each row SHALL show event name, event date, deadline date (formatted as "Uzávěrka: DD. MM. YYYY"), and a "Přihlásit se" action that opens the customized in-place registration dialog (the same flow as the events list and the event detail page), allowing the user to complete the registration without leaving the dashboard.

If no events match the criteria, the widget SHALL NOT be rendered at all (no empty-state placeholder).

#### Scenario: Member with deadlines closing this week sees the widget populated

- **GIVEN** a member who is not registered to two active events whose deadlines are 3 and 5 days from today
- **WHEN** the member opens the home dashboard
- **THEN** the "Končící přihlášky tento týden" widget is visible
- **AND** the widget lists those two events ordered by deadline ascending

#### Scenario: Member with no deadlines closing this week does not see the widget

- **GIVEN** a member who has no active events with deadlines in the next seven days that they are not registered to
- **WHEN** the member opens the home dashboard
- **THEN** the "Končící přihlášky tento týden" widget is not rendered
- **AND** no empty-state placeholder is shown

#### Scenario: Widget excludes events the member is already registered to

- **GIVEN** an active event has a deadline 4 days from today and the member is already registered to it
- **WHEN** the member opens the home dashboard
- **THEN** that event does NOT appear in the "Končící přihlášky tento týden" widget

#### Scenario: Widget includes only ACTIVE events

- **GIVEN** a DRAFT or CANCELLED event has a deadline within the next seven days
- **WHEN** the member opens the home dashboard
- **THEN** that event does NOT appear in the widget

#### Scenario: Widget shows up to five events

- **GIVEN** a member is not registered to seven active events with deadlines within the next seven days
- **WHEN** the member opens the home dashboard
- **THEN** the widget shows the five events with the earliest deadlines
- **AND** offers a "Zobrazit všechny" action that opens the events list with a filter for upcoming deadlines and "not registered by me"

#### Scenario: User without a member profile does not see the widget

- **WHEN** a user who has no member profile opens the home dashboard
- **THEN** the "Končící přihlášky tento týden" widget is not rendered

#### Scenario: Clicking an event in the widget opens the event detail

- **WHEN** a member clicks one of the listed events in the widget
- **THEN** the event detail page for that event opens

#### Scenario: Clicking the "Přihlásit se" action opens the customized in-place registration dialog

- **GIVEN** the widget lists an event that offers registration
- **WHEN** a member clicks the "Přihlásit se" action on that row
- **THEN** the customized registration dialog opens in place over the dashboard (the same dialog as on the events list, with the member's SI card number prefilled and the event and deadline context shown)
- **AND** the dashboard page does not navigate away
- **AND** the shared-service choices are shown exactly when the event offers them, as required by the event-registrations specification

#### Scenario: Registering from the widget refreshes the widget list

- **GIVEN** the member submits a registration from the dialog opened from the widget
- **WHEN** the registration is accepted
- **THEN** the widget list is refreshed so the newly registered event no longer appears among the closing registrations
