## 1. Domain Model

- [x] 1.1 Create `DeactivationReason` enum (ODHLASKA, PRESTUP, OTHER)
- [x] 1.2 Add termination fields to `Member` aggregate (deactivationReason, deactivatedAt, deactivationNote, deactivatedBy)
- [x] 1.3 Create `TerminateMembership` command record
- [x] 1.4 Create `MemberTerminatedEvent` domain event
- [x] 1.5 Implement `Member.handle(TerminateMembership)` domain method with validation

## 2. Database Layer

- [x] 2.1 Add migration script V001__add_member_termination_fields.sql with new columns
- [x] 2.2 Update `MemberMemento` to include termination fields
- [x] 2.3 Update `MemberMemento.toMember()` reconstruction to handle termination fields
- [x] 2.4 Verify Spring Data JDBC persists new fields correctly

## 3. Unit Tests (Domain Layer)

- [x] 3.1 Write test for terminating active member successfully
- [x] 3.2 Write test for rejecting termination of already terminated member
- [x] 3.3 Write test for MemberTerminatedEvent publication
- [x] 3.4 Write test for validation (missing reason, invalid reason)

## 4. Application Layer

- [x] 4.1 Create `TerminateMembershipRequest` DTO
- [x] 4.2 Create `MembershipTerminationResponse` DTO
- [x] 4.3 Implement `ManagementService.terminateMember()` method
- [x] 4.4 Add authorization check (MEMBERS:UPDATE permission)
- [x] 4.5 Add domain validation (check not already terminated)
- [x] 4.6 Add optimistic locking handling for concurrent terminations

## 5. Integration Tests (Application Layer)

- [x] 5.1 Write test for successful termination flow
- [x] 5.2 Write test for terminating already terminated member (400 Bad Request)
- [x] 5.3 Write test for concurrent termination attempts (409 Conflict)
- [x] 5.4 Write test for unauthorized termination attempt (403 Forbidden)

## 6. API Layer (Controller)

- [x] 6.1 Add `POST /api/members/{id}/terminate` endpoint to `MemberController`
- [x] 6.2 Add HATEOAS `terminate` link to active member resources
- [x] 6.3 Remove `terminate` link from terminated member resources
- [x] 6.4 Update `MemberDetailsResponse` to include termination fields
- [x] 6.5 Update `MemberMapper` to map termination fields to DTOs

## 7. Controller Tests

- [x] 7.1 Write test for successful termination returns 200 OK
- [x] 7.2 Write test for response includes termination details
- [x] 7.3 Write test for response includes HATEOAS links
- [x] 7.4 Write test for unauthorized termination returns 403
- [x] 7.5 Write test for invalid request returns 400

## 8. HATEOAS Affordances

- [x] 8.1 Add terminate affordance to HAL+FORMS template for active members
- [x] 8.2 Remove terminate affordance from terminated member templates
- [x] 8.3 Verify `self` link is present on all responses

## 9. Domain Event Integration

- [x] 9.1 Write test for MemberTerminatedEvent is published on successful termination
- [x] 9.2 Verify event contains all required fields (memberId, reason, deactivatedAt, terminatedBy)
- [x] 9.3 Add test stub for future event listeners (Finance, ORIS, CUS, Groups)

## 10. E2E Tests

- [x] 10.1 Write E2E test for complete termination workflow
- [x] 10.3 Write E2E test for terminated member in list query (filtered by active flag)

## 11. Documentation

- [x] 11.1 Update OpenAPI specification for terminate endpoint
- [x] 11.2 Add example request/response to API documentation
- [x] 11.3 Document deactivation reason enum values

## 12. Cleanup and Polish

- [x] 12.1 Run all tests and ensure >80% coverage
- [x] 12.2 Verify HATEOAS compliance on all endpoints
- [x] 12.3 Check GDPR compliance (termination data handling)
- [x] 12.4 Add label 'BackendCompleted' to GitHub issue #14
