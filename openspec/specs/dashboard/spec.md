# dashboard Specification

## Purpose

Defines the home dashboard surface — the landing page after login — including role-aware widgets that summarise the user's most relevant information (upcoming events, registrations, etc.) and provide shortcuts into the deeper parts of the application.

## Requirements

### Requirement: Upcoming Registrations Widget

The home dashboard SHALL show a "Moje nadcházející akce" widget that lists the next three events the current user is registered to, ordered by event date with the nearest event first. The widget appears only for users for whom it applies (i.e. users with a member profile); users without a member profile do not see it at all. Each listed event shows its name, location, and event date.

#### Scenario: Member with upcoming registrations sees the widget populated

- **GIVEN** a member who is registered to five future events
- **WHEN** the member opens the home dashboard
- **THEN** the "Moje nadcházející akce" widget is visible
- **AND** it shows the three events with the nearest dates first
- **AND** each row shows event name, location, and formatted event date

#### Scenario: Member with no upcoming registrations sees an empty-state CTA

- **GIVEN** a member who has no future registrations
- **WHEN** the member opens the home dashboard
- **THEN** the "Moje nadcházející akce" widget is visible
- **AND** it shows an empty-state message indicating no upcoming registrations
- **AND** it offers a shortcut labelled "Prohlédnout nadcházející akce klubu" leading to the events list

#### Scenario: User without a member profile does not see the widget

- **WHEN** a user who has no member profile opens the home dashboard
- **THEN** the "Moje nadcházející akce" widget is not rendered
- **AND** no empty-state placeholder is shown where the widget would otherwise appear

#### Scenario: Widget only includes future events

- **GIVEN** a member who is registered to past events and future events
- **WHEN** the member opens the home dashboard
- **THEN** only events whose date is today or later are listed in the widget

#### Scenario: Clicking an event in the widget opens the event detail

- **WHEN** a member clicks one of the listed events in the widget
- **THEN** the event detail page for that event opens

### Requirement: Shortcut From Widget to Full Events List

The widget SHALL offer a "Zobrazit všechny" action that navigates the user to the events list with the "Moje přihlášky" filter pre-applied and the time window set to "Budoucí", so the full list of the user's upcoming registrations is visible.

#### Scenario: Member clicks "Zobrazit všechny"

- **GIVEN** a member viewing the home dashboard
- **WHEN** the member clicks "Zobrazit všechny" on the "Moje nadcházející akce" widget
- **THEN** the events list opens
- **AND** the "Moje přihlášky" filter is active
- **AND** the time window is set to "Budoucí"

#### Scenario: Member has more than three upcoming registrations

- **GIVEN** a member who is registered to seven future events
- **WHEN** the member opens the home dashboard
- **THEN** the widget shows the first three events
- **AND** the "Zobrazit všechny" action is visible as the way to see the remaining four

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

The widget SHALL list at most five events, ordered by registration deadline ascending (the soonest-closing first). Each row SHALL show event name, event date, deadline date (formatted as "Uzávěrka: DD. MM. YYYY"), and a "Přihlásit se" action that opens the in-place registration form (same flow as the events list), allowing the user to complete the registration without leaving the dashboard.

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
