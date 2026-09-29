## MODIFIED Requirements

### Requirement: User Authentication

The system SHALL authenticate users via OAuth2 using a login name and a cryptographically hashed password. Club members SHALL log in with their registration number. Legal guardians who are not club members SHALL log in with their e-mail address. Authorities are loaded from the UserPermissions aggregate.

#### Scenario: User logs in with valid credentials

- **WHEN** user enters their registration number and password on the login page and submits
- **THEN** the system issues an access token and a refresh token
- **AND** the user is redirected to the application

#### Scenario: Non-member guardian logs in with e-mail address

- **WHEN** a non-member guardian enters their e-mail address and password on the login page and submits
- **THEN** the system issues an access token and a refresh token
- **AND** the guardian is redirected to the application

#### Scenario: Club member cannot log in with e-mail address

- **WHEN** a club member enters their e-mail address instead of their registration number on the login page
- **THEN** the login page shows an error that the credentials are invalid

#### Scenario: Login page explains how guardians log in

- **WHEN** a person opens the login page
- **THEN** the login name field explains that members use their registration number and guardians who are not members use their e-mail address

#### Scenario: User logs in with invalid credentials

- **WHEN** user enters an incorrect registration number or password
- **THEN** the login page shows an error that the credentials are invalid

#### Scenario: Expired access token is rejected

- **WHEN** the user's access token has expired
- **THEN** the system automatically attempts to refresh using the refresh token, or prompts the user to log in again

### Requirement: User Aggregate

The system SHALL manage user accounts as a separate aggregate from members. Users cannot be deleted, only disabled. The User aggregate stores only identity-related data (credentials, account status) — not authorities. A user account SHALL exist for every member and for every legal guardian who is not a club member. The e-mail address used as login name of a non-member guardian SHALL be unique among such accounts.

#### Scenario: User account is created during member registration

- **WHEN** a new member is registered
- **THEN** a user account is automatically created with a generated unique identifier
- **AND** the account is created in pending activation status

#### Scenario: User account is created for a new non-member guardian

- **WHEN** a minor is registered, or a guardian is added to a minor, with a guardian who is not a club member and whose e-mail address is not yet used by another guardian account
- **THEN** a user account with the guardian's e-mail address as login name is created in pending activation status

#### Scenario: Existing guardian account is reused

- **WHEN** a guardian is entered whose e-mail address already belongs to a non-member guardian account
- **THEN** no new user account is created
- **AND** the existing guardian is linked to the minor

#### Scenario: Changing a non-member guardian's e-mail changes their login name

- **WHEN** an admin changes the e-mail address of a non-member guardian
- **THEN** the guardian logs in with the new e-mail address from then on

#### Scenario: Guardian e-mail already used by another guardian account is rejected

- **WHEN** an admin changes a non-member guardian's e-mail address to one already used by a different guardian account
- **THEN** the form shows an error that the e-mail address is already in use

#### Scenario: User account can be suspended

- **WHEN** admin suspends a user account
- **THEN** subsequent login attempts with that account fail
- **AND** the associated member profile (if any) remains in the database

### Requirement: Password Setup Token Reissuance

The system SHALL allow users with PENDING_ACTIVATION status to request an activation link, with rate limiting (3 requests per hour, minimum 10 minutes between requests). A club member SHALL identify their account by registration number and e-mail address; the link SHALL be sent only when the entered e-mail address matches the member's own e-mail address held in Klabis. A non-member guardian SHALL identify their account by their e-mail address alone. The account of a minor SHALL NOT be activated through this form; it is started only with the "Založit účet" action. The response SHALL look the same whether or not a link was sent, so the form cannot reveal which e-mail addresses or accounts exist.

#### Scenario: Member reaches account activation from the login page

- **WHEN** a person on the login page chooses to activate their account
- **THEN** they are taken to a form asking for their login name (registration number or, for guardians, e-mail address) and e-mail address

#### Scenario: Member requests an activation link with their own e-mail

- **WHEN** an adult member submits their registration number and the e-mail address held in Klabis for them
- **AND** they have not exceeded the rate limit
- **THEN** an activation link is sent to that e-mail address
- **AND** the page confirms that a link will arrive if the account is awaiting activation

#### Scenario: Non-member guardian requests an activation link

- **WHEN** a non-member guardian whose account awaits activation submits their e-mail address
- **AND** the rate limit has not been exceeded
- **THEN** an activation link is sent to that e-mail address
- **AND** the page shows the same confirmation as for any other request

#### Scenario: Guardian's e-mail no longer activates a minor's account

- **WHEN** someone submits a minor's registration number together with the e-mail address of the minor's guardian
- **THEN** no activation link is sent anywhere
- **AND** the page shows the same confirmation as for a matching request

#### Scenario: Minor's own e-mail does not start the minor's account

- **WHEN** someone submits a minor's registration number together with the minor's own e-mail address
- **THEN** no activation link is sent
- **AND** the page shows the same confirmation as for a matching request

#### Scenario: Request with an e-mail that does not belong to the member

- **WHEN** someone submits a registration number together with an e-mail address that is not the member's own
- **THEN** no activation link is sent anywhere
- **AND** the page shows the same confirmation as for a matching request

#### Scenario: Request for a member without any e-mail in Klabis

- **WHEN** someone submits the registration number of a member for whom Klabis holds no e-mail address of their own
- **THEN** no activation link is sent
- **AND** the page shows the same confirmation as for a matching request

#### Scenario: Rate limit exceeded shows retry information

- **WHEN** member has already requested 3 tokens in the past hour
- **THEN** the system shows an error with information about when they can try again

#### Scenario: Account already active shows appropriate message

- **WHEN** member with an already-active account requests a new activation token
- **THEN** the system shows that the account is already active and directs them to log in
