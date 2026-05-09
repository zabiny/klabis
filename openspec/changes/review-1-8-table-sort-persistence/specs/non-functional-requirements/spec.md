## ADDED Requirements

### Requirement: Tables Persist User Sort Preference

The data tables in the application (events, members, family groups, training groups, free groups, calendar items, and similar listings) SHALL persist the user's last applied sort preference per table, so that returning to the same table — after page reload, in a new browser tab, or in a later session — restores that sort.

The persistence is per browser (no server-side cross-device sync), per table, and applies to sort only. Filter state, page size, and column visibility are NOT covered by this requirement.

When a URL contains an explicit sort parameter (e.g. a shared link), the URL value SHALL take precedence over the persisted preference, so shared links continue to render the same view for everyone. Once the user changes the sort, both the URL and the persisted preference SHALL be updated.

The user SHALL have an action to reset the table to its default sort, which clears the persisted preference for that table.

#### Scenario: Sort preference persists across page reload

- **GIVEN** a member opens the events list and sorts by name ascending
- **WHEN** the member reloads the page
- **THEN** the events list opens with sort by name ascending

#### Scenario: Sort preference persists across browser sessions

- **GIVEN** a member sorts the members list by registration number descending
- **AND** the member closes the browser
- **WHEN** the member opens the application the next day in the same browser and navigates to the members list
- **THEN** the members list opens with sort by registration number descending

#### Scenario: Sort preference is per table, not global

- **GIVEN** a member sorts the events list by date and the members list by last name
- **WHEN** the member returns to each table separately
- **THEN** each table retains its own sort independently

#### Scenario: Shared URL with explicit sort overrides persisted preference

- **GIVEN** a member has a persisted sort preference for the events list (e.g. by name)
- **WHEN** the member opens a shared link with an explicit URL sort parameter (e.g. `?sort=date,desc`)
- **THEN** the table renders sorted by the URL parameter (date descending), not the persisted preference

#### Scenario: User can reset to default sort

- **GIVEN** a member has a persisted sort preference for a table different from the default
- **WHEN** the member triggers the "Resetovat řazení" action on the table
- **THEN** the persisted preference for that table is cleared
- **AND** the table renders with its default sort

#### Scenario: Sort preference is per browser, not synced across devices

- **GIVEN** a member sorts the events list by name in browser A
- **WHEN** the member opens the events list in browser B (or another device, signed in as the same user)
- **THEN** browser B opens the events list with the default sort
- **AND** browser A retains its sort preference unchanged
