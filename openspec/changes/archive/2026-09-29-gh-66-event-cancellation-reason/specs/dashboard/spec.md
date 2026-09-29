## MODIFIED Requirements

### Requirement: Upcoming Registrations Widget

The home dashboard SHALL show a "Moje nadcházející akce" widget that lists the next three active events (status ACTIVE) the current user is registered to, ordered by event date with the nearest event first. The widget appears only for users for whom it applies (i.e. users with a member profile); users without a member profile do not see it at all. Each listed event shows its name, location, and event date.

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

#### Scenario: Cancelled event is not listed in the widget

- **GIVEN** a member registered to a future event that has been cancelled
- **WHEN** the member opens the home dashboard
- **THEN** the cancelled event is not listed in the "Moje nadcházející akce" widget

#### Scenario: Clicking an event in the widget opens the event detail

- **WHEN** a member clicks one of the listed events in the widget
- **THEN** the event detail page for that event opens
