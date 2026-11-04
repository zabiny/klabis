## ADDED Requirements

### Requirement: Return to the Originally Requested Page After Sign-In

When a user who is not signed in opens a page of the application, the system SHALL show them that page once sign-in completes, instead of the home page. This SHALL hold both when the user fills in the login form and when sign-in completes without it. Only pages of the application itself SHALL be restored; anything else SHALL lead to the home page.

#### Scenario: Member opens a bookmarked page and signs in on the login form

- **GIVEN** a member who is not signed in
- **WHEN** the member opens a bookmarked link to the detail of an event
- **AND** the login page is shown and the member signs in with valid credentials
- **THEN** the event detail is shown, not the home page

#### Scenario: Member opens a shared link while still signed in at the login service

- **GIVEN** a member who signed in earlier in this browser but whose application session has ended
- **WHEN** the member opens a shared link to the membership fee tiers page
- **THEN** the member is signed in again without the login page being shown
- **AND** the membership fee tiers page is shown

#### Scenario: Requested search and filter settings are kept

- **WHEN** a member who is not signed in opens a link to a list page that includes search or filter settings in the address
- **AND** completes sign-in
- **THEN** the list page is shown with the same search and filter settings

#### Scenario: Member returns to the page they were on after the session expires

- **GIVEN** a member is working on an event detail
- **WHEN** their session can no longer be renewed and the member signs in again
- **THEN** the same event detail is shown after sign-in

#### Scenario: Member opening the application without a specific page lands on the home page

- **WHEN** a member who is not signed in opens the application address without any specific page
- **AND** completes sign-in
- **THEN** the home page is shown

#### Scenario: Sign-in never leads to a page outside the application

- **WHEN** sign-in completes with a return target that is not a page of the application (for example a page of another website)
- **THEN** the home page of the application is shown instead

#### Scenario: Next sign-in after logout starts on the home page

- **GIVEN** a member logs out while on an event detail
- **WHEN** a user signs in again in the same browser
- **THEN** the home page is shown, not the event detail the previous member was on
