# Change: Add GET /api/members/{id} Endpoint

## Why

The member list endpoint (`GET /api/members`) only returns summary information (firstName, lastName,
registrationNumber). Users need the ability to retrieve full details of a specific member including all personal
information, contact details, and guardian information. Currently, there is a placeholder endpoint that throws
`UnsupportedOperationException`.

## What Changes

- Implement the `GET /api/members/{id}` endpoint to return complete member details
- Create a new query handler (`GetMemberQueryHandler`) in the application layer
- Create a new DTO (`MemberDetailsDTO`) to carry full member information
- Add HATEOAS links to the response (self, collection, edit)
- Handle 404 Not Found when member doesn't exist
- Enforce authorization (MEMBERS:READ permission)

## Impact

- **Affected specs**: members
- **Affected code**:
    - `com.klabis.members.presentation.MemberController` (remove placeholder, implement endpoint)
    - New: `com.klabis.members.application.GetMemberQueryHandler`
    - New: `com.klabis.members.application.GetMemberQuery`
    - New: `com.klabis.members.application.MemberDetailsDTO`
    - New: `com.klabis.members.presentation.MemberDetailsResponse`
    - Tests: `com.klabis.members.presentation.GetMemberApiTest`
