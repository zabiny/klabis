## ADDED Requirements

### Requirement: Registering A Non-Member Guardian As A Member

The system SHALL let a user with MEMBERS:MANAGE permission start the registration of a non-member legal guardian as a club member directly from the guardian's profile. The registration form then opens with this guardian already chosen for takeover and the guardian's data pre-filled; it otherwise behaves as the regular member registration with a guardian takeover.

#### Scenario: Admin starts the registration from the guardian's profile

- **GIVEN** a non-member legal guardian "Eva Svobodová"
- **WHEN** an admin opens her profile and clicks "Registrovat jako člena"
- **THEN** the member registration form opens with "Eva Svobodová" chosen in the "Převzetí zákonného zástupce" section
- **AND** her first name, last name, e-mail and telephone are pre-filled

#### Scenario: Admin changes pre-filled data before submitting

- **GIVEN** the registration form opened from the profile of "Eva Svobodová"
- **WHEN** the admin changes the pre-filled telephone, fills in the remaining data of an adult and submits the form
- **THEN** "Eva Svobodová" is registered as a club member with the changed telephone

#### Scenario: Admin lands on the new member's detail after submitting

- **GIVEN** the registration form opened from the profile of "Eva Svobodová"
- **WHEN** the admin submits the form with valid data of an adult
- **THEN** the detail page of the new member "Eva Svobodová" opens with her new registration number
- **AND** she remains the legal guardian of her minors
- **AND** she can still log in with her former login number EXTnnnn

#### Scenario: Cancelling returns to the guardian's profile

- **GIVEN** the registration form opened from the profile of "Eva Svobodová"
- **WHEN** the admin clicks "Zrušit"
- **THEN** the profile of "Eva Svobodová" is shown again
- **AND** she is still a non-member legal guardian

#### Scenario: Guardian does not see the action on their own profile

- **WHEN** a logged-in non-member guardian opens "Můj profil"
- **THEN** no "Registrovat jako člena" action is shown
