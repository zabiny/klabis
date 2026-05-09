## ADDED Requirements

### Requirement: Dashboard Begins With Content, Not With a Welcome Block

The home dashboard SHALL begin directly with its first content widget. No welcome heading addressing the user by name and no application tagline (e.g. a generic marketing description) SHALL be rendered above the widgets.

User personalization (current user identity) SHALL be conveyed through the persistent top bar, not through a welcome heading on the dashboard.

#### Scenario: Member opens the home dashboard

- **WHEN** an authenticated member opens the home dashboard after login
- **THEN** the page does NOT display a heading like "Vítejte v Klabis, [name]"
- **AND** the page does NOT display the tagline "Moderní systém pro správu členského klubu"
- **AND** the first visible content is one of the dashboard widgets

### Requirement: Upcoming Deadlines Widget

The home dashboard SHALL show a "Končící přihlášky tento týden" widget that lists active events (status ACTIVE) whose nearest future registration deadline falls within the next seven days (today through today+7), and to which the current user is NOT yet registered. The widget appears only for users with a member profile.

The widget SHALL list at most five events, ordered by registration deadline ascending (the soonest-closing first). Each row SHALL show event name, event date, deadline date (formatted as "Uzávěrka: DD. MM."), and a "Přihlásit se" action that opens the event detail to complete the registration.

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

#### Scenario: Clicking the "Přihlásit se" action opens the event detail

- **WHEN** a member clicks the "Přihlásit se" action on a row in the widget
- **THEN** the event detail page for that event opens with the registration form prepared
