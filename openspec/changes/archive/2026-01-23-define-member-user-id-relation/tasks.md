## 1. Implementation

- [x] 1.1 Create UserId Java record in users.domain package
- [x] 1.2 Define record components: UUID uuid
- [x] 1.3 Implement compact constructor with UUID null validation
- [x] 1.4 Implement static factory method: fromString(String uuidString)
- [x] 1.5 Add UUID string validation in fromString() method
- [x] 1.6 Review current Member aggregate ID generation implementation
- [x] 1.7 Review current User aggregate implementation and ID structure
- [x] 1.8 Update Member aggregate to use UserId record instead of UUID
- [x] 1.9 Update User aggregate to use UserId record instead of UUID
- [x] 1.10 Update RegisterMemberCommandHandler to create User first, then Member with shared UserId
- [x] 1.11 Update database constraints to establish shared ID relationship
- [x] 1.12 Optimize repository queries to leverage shared UserId (no joins needed)
- [x] 1.13 Write unit tests for UserId record (null validation, fromString, equals/hashCode)
- [x] 1.14 Write unit tests for RegisterMemberCommandHandler User-then-Member creation flow
- [x] 1.15 Write integration tests for user-member UserId mapping
- [x] 1.16 Write test for User creation failure preventing Member creation
- [x] 1.17 Update code documentation (RegisterMemberCommandHandler, aggregates)

## 2. Documentation

- [x] 2.1 Update members specification with clarified ID requirements (done via this proposal)
- [x] 2.2 Update auth specification to document Member-User ID relationship (done via this proposal)
- [x] 2.3 Add architectural decision record (ADR) for UserId Java record design pattern (updated ARCHITECTURE.md,
  API.md, README.md)
