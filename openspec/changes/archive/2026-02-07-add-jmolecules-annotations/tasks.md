## 1. Maven Dependencies

- [x] 1.1 Add `jmolecules-bom` (version `2025.0.2`) to `dependencyManagement` section in `pom.xml`
- [x] 1.2 Add `jmolecules-ddd` dependency
- [x] 1.3 Add `jmolecules-hexagonal-architecture` dependency
- [x] 1.4 Add `jmolecules-archunit` test dependency
- [x] 1.5 Verify project compiles with new dependencies (`mvn compile`)

## 2. Events Module - DDD Annotations

- [x] 2.1 Add `@AggregateRoot` and `@Identity` to `Event` (corrected: added `@Identity` on id field)
- [x] 2.2 Add `@ValueObject` and `implements Identifier` to `EventId`
- [x] 2.3 Add `@ValueObject` to `SiCardNumber`, `WebsiteUrl`, `EventRegistration`
- [x] 2.4 Add `@DomainEvent` to `EventCreatedEvent`, `EventPublishedEvent`, `EventCancelledEvent`, `EventFinishedEvent`,
  `MemberRegisteredForEventEvent`, `MemberUnregisteredFromEventEvent`
- [x] 2.5 Add `@Repository` to `EventRepository`
- [x] 2.6 Add `@Service` and `@PrimaryPort` to `EventManagementService`, `EventRegistrationService` (corrected: services
  are primary ports)
- [x] 2.7 Add `@Association` to cross-aggregate references (MemberId fields in Event/EventRegistration)

## 3. Events Module - Hexagonal Annotations

- [x] 3.1 ~~Add `@PrimaryPort` to `Events` (public query API)~~ (corrected: removed `@PrimaryPort` - query interfaces
  are NOT primary ports, they are implemented by secondary adapters)
- [x] 3.2 Add `@SecondaryPort` to `EventRepository`
- [x] 3.3 Add `@PrimaryAdapter` to `EventController`, `EventRegistrationController`
- [x] 3.4 Add `@SecondaryAdapter` to `EventRepositoryAdapter`

## 4. Members Module - DDD Annotations

- [x] 4.1 Add `@AggregateRoot` and `@Identity` to `Member` (corrected: added `@Identity` on id field)
- [x] 4.2 Add `@ValueObject` and `implements Identifier` to `MemberId`
- [x] 4.3 Add `@ValueObject` to `RegistrationNumber`, `PersonName`, `EmailAddress`, `PhoneNumber`, `Address`,
  `Nationality`, `PersonalInformation`, `GuardianInformation`, `IdentityCard`, `ExpiringDocument`, `MedicalCourse`,
  `TrainerLicense`, `AuditMetadata`
- [x] 4.4 Add `@DomainEvent` to `MemberCreatedEvent`
- [x] 4.5 Add `@Repository` to `MemberRepository`
- [x] 4.6 Add `@Service` and `@PrimaryPort` to `ManagementService`, `RegistrationService` (corrected: services are
  primary ports)

## 5. Members Module - Hexagonal Annotations

- [x] 5.1 ~~Add `@PrimaryPort` to `Members` (public query API)~~ (corrected: removed `@PrimaryPort` - query interfaces
  are NOT primary ports)
- [x] 5.2 Add `@SecondaryPort` to `MemberRepository`
- [x] 5.3 Add `@PrimaryAdapter` to `MemberController`, `MemberCreatedEventHandler`
- [x] 5.4 Add `@SecondaryAdapter` to `MemberRepositoryAdapter`

## 6. Users Module - DDD Annotations

- [x] 6.1 Add `@AggregateRoot` and `@Identity` to `User`, `PasswordSetupToken`
- [x] 6.2 Add `@ValueObject` and `implements Identifier` to `UserId`
- [x] 6.3 Add `@ValueObject` to `TokenHash`, `ActivationToken`, `UserAuditMetadata`, `UserCreationParams`
- [x] 6.4 Add `@DomainEvent` to `UserCreatedEvent`
- [x] 6.5 Add `@Repository` to `UserRepository`, `PasswordSetupTokenRepository`, `UserPermissionsRepository`
- [x] 6.6 Add `@Service` and `@PrimaryPort` to `PasswordSetupService`, `PermissionService`
- [x] 6.7 Add `@Association` to cross-aggregate references (MemberId fields in User) (skipped: User aggregate does not
  contain MemberId references - users are linked via registration number string, not via MemberId)

## 7. Users Module - Hexagonal Annotations

- [ ] ~~7.1 Add `@PrimaryPort` to `Users` (public query API)~~ REMOVED - query interfaces are NOT primary ports,
  services are
- [x] 7.2 Add `@SecondaryPort` to `UserRepository`, `PasswordSetupTokenRepository`, `UserPermissionsRepository`
- [x] 7.3 Add `@PrimaryAdapter` to `PasswordSetupController`, `PermissionController`, `UserCreatedEventHandler` (
  PasswordSetupEventListener removed - no longer needed)
- [x] 7.4 Add `@SecondaryAdapter` to `UserRepositoryAdapter`, `PasswordSetupTokenRepositoryAdapter`,
  `UserPermissionsRepositoryAdapter`

## 8. Common Module - Hexagonal Annotations

- [x] 8.1 Add `@SecondaryPort` to `EmailService` (interface)
- [x] 8.2 Add `@SecondaryAdapter` to `JavaMailEmailService`, `LoggingEmailService`

## 9. ArchUnit Tests

- [x] 9.1 Create `src/test/java/com/klabis/architecture/JMoleculesArchitectureTest.java`
- [x] 9.2 Add test: `dddBuildingBlocksShouldBeValid()` using `JMoleculesDddRules.all()`
- [x] 9.3 Add test: `hexagonalArchitectureShouldBeRespected()` using `JMoleculesArchitectureRules.ensureHexagonal()`
- [x] 9.4 Remove unused `PasswordSetupEventListener` and its test (architecture violation fix)
- [x] 9.5 Verify all tests pass (`mvn test`)
