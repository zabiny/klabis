## MODIFIED Requirements

### Requirement: HATEOAS Link Structure

The system SHALL include hypermedia links in API responses reflecting the current user's authorized actions.

#### Scenario: Member creation response includes hypermedia links

- **WHEN** a member is created successfully
- **THEN** the response includes a `self` link to the member resource
- **AND** the response includes an `edit` link if the user has MEMBERS:MANAGE permission
- **AND** the response includes a `collection` link to the members list

#### Scenario: Unauthorized user receives limited links

- **WHEN** user without edit permission views a member
- **THEN** the response includes the `self` link
- **AND** the response does not include the `edit` link

#### Scenario: Member details response includes permissions link for authorized user

- **WHEN** user with MEMBERS:PERMISSIONS authority views a member detail
- **THEN** the response includes a `permissions` link pointing to `/api/users/{id}/permissions`

#### Scenario: Permissions link excluded for unauthorized user

- **WHEN** user without MEMBERS:PERMISSIONS authority views a member detail
- **THEN** the response does not include a `permissions` link

#### Scenario: Member details response includes suspend link for active member

- **WHEN** user with MEMBERS:MANAGE permission views an active member
- **THEN** the response includes a `suspend` link

#### Scenario: Suspend link excluded for suspended member

- **WHEN** user views a suspended member
- **THEN** the response does not include a `suspend` link

#### Scenario: Event summary includes registration affordance when open

- **WHEN** an event in the list has open registrations and the current user is not registered
- **THEN** the event summary self link includes a `registerForEvent` affordance

#### Scenario: Event summary includes unregistration affordance when registered

- **WHEN** an event in the list has open registrations and the current user is already registered
- **THEN** the event summary self link includes an `unregisterFromEvent` affordance

#### Scenario: DRAFT event links for manager

- **WHEN** user with EVENTS:MANAGE permission views a DRAFT event
- **THEN** response includes links: self, edit, publish, cancel, registrations

#### Scenario: ACTIVE event links for manager

- **WHEN** user with EVENTS:MANAGE permission views an ACTIVE event
- **THEN** response includes links: self, edit, cancel, finish, registrations

#### Scenario: FINISHED or CANCELLED event links

- **WHEN** user views a FINISHED or CANCELLED event
- **THEN** response includes links: self, registrations
- **AND** edit, publish, cancel, finish links are not included

#### Scenario: Event-linked calendar item includes link to event

- **WHEN** an event-linked calendar item is returned
- **THEN** response includes `_links.event.href` pointing to `/api/events/{eventId}`

#### Scenario: Calendar response includes month navigation links

- **WHEN** user views calendar items for June 2026
- **THEN** response includes HATEOAS `next` link with startDate=2026-07-01&endDate=2026-07-31
- **AND** response includes HATEOAS `prev` link with startDate=2026-05-01&endDate=2026-05-31
- **AND** links preserve the same sort parameter as the original request

#### Scenario: Member list response includes pagination links

- **WHEN** the list members endpoint returns a paginated response
- **THEN** the response `_links` object includes: self, first, last
- **AND** includes `prev` link if not on the first page
- **AND** includes `next` link if not on the last page
- **AND** all pagination links preserve the sort and size query parameters
