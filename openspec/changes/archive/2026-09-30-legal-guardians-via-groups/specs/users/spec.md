## MODIFIED Requirements

### Requirement: User Authentication

The system SHALL authenticate users via OAuth2 using a login name and a cryptographically hashed password. The login name of a club member is their registration number; the login name of a non-member legal guardian is their login number from the EXT series (EXT0001, EXT0002, …), which has the same format as a registration number and is kept if the guardian later becomes a club member. Authorities are loaded from the UserPermissions aggregate.

#### Scenario: User logs in with valid credentials

- **WHEN** user enters their registration number and password on the login page and submits
- **THEN** the system issues an access token and a refresh token
- **AND** the user is redirected to the application

#### Scenario: Non-member legal guardian logs in with login number

- **WHEN** a non-member legal guardian enters their login number (for example EXT0001) and password on the login page and submits
- **THEN** the guardian is logged in and redirected to the application

#### Scenario: User logs in with invalid credentials

- **WHEN** user enters an incorrect login name or password
- **THEN** the login page shows an error that the credentials are invalid

#### Scenario: Expired access token is rejected

- **WHEN** the user's access token has expired
- **THEN** the system automatically attempts to refresh using the refresh token, or prompts the user to log in again

### Requirement: User Aggregate

The system SHALL manage user accounts as a separate aggregate from members and legal guardians. Users cannot be deleted, only disabled. The User aggregate stores only identity-related data (credentials, account status) — not authorities.

#### Scenario: User account is created during member registration

- **WHEN** a new member is registered
- **THEN** a user account is automatically created with a generated unique identifier
- **AND** the account is created in pending activation status

#### Scenario: User account is created for a new non-member legal guardian

- **WHEN** a new non-member legal guardian is entered
- **THEN** a user account with the next login number from the EXT series is created in pending activation status
- **AND** the account holds only the permission to view club members

#### Scenario: User account can be suspended

- **WHEN** admin suspends a user account
- **THEN** subsequent login attempts with that account fail
- **AND** the associated member profile (if any) remains in the database

### Requirement: Password Setup Token Generation

The system SHALL generate a secure, time-limited password setup token (valid for 4 hours) when a user with PENDING_ACTIVATION status requests an activation link, or when an administrator creates a minor's own account. Creating a user account SHALL NOT by itself generate a token or send any e-mail.

#### Scenario: No activation e-mail is sent when an account is created

- **WHEN** a new member or a new non-member legal guardian is registered, by hand or brought in from ORIS
- **THEN** a user account awaiting activation is created
- **AND** no password setup e-mail is sent

#### Scenario: Account is created for a member without any e-mail

- **WHEN** a member with no e-mail address is brought into Klabis
- **THEN** a user account awaiting activation is created successfully
- **AND** no password setup e-mail is sent

#### Scenario: Token is generated when an activation link is requested

- **WHEN** an adult member or a non-member legal guardian with a pending account requests an activation link with a matching e-mail address
- **THEN** a secure password setup token valid for 4 hours is generated
- **AND** the activation link is sent to the e-mail address they entered

#### Scenario: Token is generated when an admin creates a minor's account

- **WHEN** an admin uses "Založit účet" for a minor with an own e-mail
- **THEN** a secure password setup token valid for 4 hours is generated
- **AND** the activation link is sent to the minor's own e-mail

### Requirement: Password Setup Token Reissuance

The system SHALL allow users with PENDING_ACTIVATION status to request an activation link, with rate limiting (3 requests per hour, minimum 10 minutes between requests). The link SHALL be sent only when the e-mail address entered matches the account holder's own e-mail as held in Klabis: an adult member's own e-mail, or a non-member legal guardian's e-mail. No link SHALL be sent through this request for the account of a minor, neither to the minor's nor to a guardian's e-mail. The response SHALL look the same whether or not a link was sent, so the form cannot reveal which e-mail addresses or accounts exist.

#### Scenario: Member reaches account activation from the login page

- **WHEN** a person on the login page chooses to activate their account
- **THEN** they are taken to a form asking for their registration number and e-mail address

#### Scenario: Adult member requests an activation link with their own e-mail

- **WHEN** an adult member submits their registration number and the e-mail address held in Klabis for them
- **AND** they have not exceeded the rate limit
- **THEN** an activation link is sent to that e-mail address
- **AND** the page confirms that a link will arrive if the account is awaiting activation

#### Scenario: Non-member guardian requests an activation link

- **WHEN** a non-member legal guardian submits their login number and their e-mail address held in Klabis
- **AND** the rate limit has not been exceeded
- **THEN** an activation link is sent to the guardian's e-mail address

#### Scenario: No self-service activation for a minor

- **WHEN** someone submits a minor's registration number together with the minor's own e-mail or a guardian's e-mail
- **THEN** no activation link is sent anywhere
- **AND** the page shows the same confirmation as for a matching request

#### Scenario: Request with an e-mail that does not belong to the account holder

- **WHEN** someone submits a login name together with an e-mail address that is not the account holder's own
- **THEN** no activation link is sent anywhere
- **AND** the page shows the same confirmation as for a matching request

#### Scenario: Request for a member without any e-mail in Klabis

- **WHEN** someone submits the registration number of a member for whom Klabis holds no e-mail address
- **THEN** no activation link is sent
- **AND** the page shows the same confirmation as for a matching request

#### Scenario: Rate limit exceeded shows retry information

- **WHEN** member has already requested 3 tokens in the past hour
- **THEN** the system shows an error with information about when they can try again

#### Scenario: Account already active shows appropriate message

- **WHEN** member with an already-active account requests a new activation token
- **THEN** the system shows that the account is already active and directs them to log in
