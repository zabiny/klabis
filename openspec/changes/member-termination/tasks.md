## 1. Domain Model

- [ ] 1.1 Create `DeactivationReason` enum (ODHLASKA, PRESTUP, OTHER)
- [ ] 1.2 Add termination fields to `Member` aggregate (deactivationReason, deactivatedAt, deactivationNote, deactivatedBy)
- [ ] 1.3 Create `TerminateMembership` command record
- [ ] 1.4 Create `MemberTerminatedEvent` domain event
- [ ] 1.5 Implement `Member.handle(TerminateMembership)` domain method with validation

## 2. Database Layer

- [ ] 2.1 Add migration script V001__add_member_termination_fields.sql with new columns
- [ ] 2.2 Update `MemberMemento` to include termination fields
- [ ] 2.3 Update `MemberMemento.toMember()` reconstruction to handle termination fields
- [ ] 2.4 Verify Spring Data JDBC persists new fields correctly

## 3. Unit Tests (Domain Layer)

- [ ] 3.1 Write test for terminating active member successfully
- [ ] 3.2 Write test for rejecting termination of already terminated member
- [ ] 3.3 Write test for MemberTerminatedEvent publication
- [ ] 3.4 Write test for validation (missing reason, invalid reason)

## 4. Application Layer

- [ ] 4.1 Create `TerminateMembershipRequest` DTO
- [ ] 4.2 Create `MembershipTerminationResponse` DTO
- [ ] 4.3 Implement `ManagementService.terminateMember()` method
- [ ] 4.4 Add authorization check (MEMBERS:UPDATE permission)
- [ ] 4.5 Add domain validation (check not already terminated)
- [ ] 4.6 Add optimistic locking handling for concurrent terminations

## 5. Integration Tests (Application Layer)

- [ ] 5.1 Write test for successful termination flow
- [ ] 5.2 Write test for terminating already terminated member (400 Bad Request)
- [ ] 5.3 Write test for concurrent termination attempts (409 Conflict)
- [ ] 5.4 Write test for unauthorized termination attempt (403 Forbidden)

## 6. API Layer (Controller)

- [ ] 6.1 Add `POST /api/members/{id}/terminate` endpoint to `MemberController`
- [ ] 6.2 Add HATEOAS `terminate` link to active member resources
- [ ] 6.3 Remove `terminate` link from terminated member resources
- [ ] 6.4 Update `MemberDetailsResponse` to include termination fields
- [ ] 6.5 Update `MemberMapper` to map termination fields to DTOs

## 7. Controller Tests

- [ ] 7.1 Write test for successful termination returns 200 OK
- [ ] 7.2 Write test for response includes termination details
- [ ] 7.3 Write test for response includes HATEOAS links
- [ ] 7.4 Write test for unauthorized termination returns 403
- [ ] 7.5 Write test for invalid request returns 400

## 8. HATEOAS Affordances

- [ ] 8.1 Add terminate affordance to HAL+FORMS template for active members
- [ ] 8.2 Remove terminate affordance from terminated member templates
- [ ] 8.3 Verify `self` link is present on all responses

## 9. Domain Event Integration

- [ ] 9.1 Write test for MemberTerminatedEvent is published on successful termination
- [ ] 9.2 Verify event contains all required fields (memberId, reason, deactivatedAt, terminatedBy)
- [ ] 9.3 Add test stub for future event listeners (Finance, ORIS, CUS, Groups)

## 10. E2E Tests

- [ ] 10.1 Write E2E test for complete termination workflow
- [ ] 10.3 Write E2E test for terminated member in list query (filtered by active flag)

## 11. Documentation

- [ ] 11.1 Update OpenAPI specification for terminate endpoint
- [ ] 11.2 Add example request/response to API documentation
- [ ] 11.3 Document deactivation reason enum values

## 12. Cleanup and Polish

- [ ] 12.1 Run all tests and ensure >80% coverage
- [ ] 12.2 Verify HATEOAS compliance on all endpoints
- [ ] 12.3 Check GDPR compliance (termination data handling)
- [ ] 12.4 Add label 'BackendCompleted' to GitHub issue #14
