## MODIFIED Requirements

### Requirement: Password Setup Token Generation

The system SHALL generate a secure, time-limited password setup token (valid for 4 hours) when a user with PENDING_ACTIVATION status requests an activation link. Creating a user account SHALL NOT by itself generate a token or send any e-mail.

#### Scenario: No activation e-mail is sent when an account is created

- **WHEN** a new member is registered, by hand or brought in from ORIS
- **THEN** a user account awaiting activation is created
- **AND** no password setup e-mail is sent

#### Scenario: Account is created for a member without any e-mail

- **WHEN** a member with no e-mail address of their own or of a guardian is brought into Klabis
- **THEN** a user account awaiting activation is created successfully
- **AND** no password setup e-mail is sent

#### Scenario: Token is generated when an activation link is requested

- **WHEN** a member with a pending account requests an activation link with a matching e-mail address
- **THEN** a secure password setup token valid for 4 hours is generated
- **AND** the activation link is sent to the e-mail address they entered

### Requirement: Password Setup Token Reissuance

The system SHALL allow users with PENDING_ACTIVATION status to request an activation link, with rate limiting (3 requests per hour, minimum 10 minutes between requests). The link SHALL be sent only when the e-mail address entered matches the member's own e-mail address or their guardian's e-mail address as held in Klabis. The response SHALL look the same whether or not a link was sent, so the form cannot reveal which e-mail addresses or accounts exist.

#### Scenario: Member reaches account activation from the login page

- **WHEN** a person on the login page chooses to activate their account
- **THEN** they are taken to a form asking for their registration number and e-mail address

#### Scenario: Member requests an activation link with their own e-mail

- **WHEN** member submits their registration number and the e-mail address held in Klabis for them
- **AND** they have not exceeded the rate limit
- **THEN** an activation link is sent to that e-mail address
- **AND** the page confirms that a link will arrive if the account is awaiting activation

#### Scenario: Guardian requests an activation link for a minor

- **WHEN** a guardian submits a minor's registration number and the guardian's e-mail address held in Klabis
- **AND** the rate limit has not been exceeded
- **THEN** an activation link is sent to the guardian's e-mail address

#### Scenario: Request with an e-mail that does not belong to the member

- **WHEN** someone submits a registration number together with an e-mail address that is neither the member's nor their guardian's
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
