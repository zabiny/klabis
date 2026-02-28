# Backend Developer Agent Memory

## @WebMvcTest + SecurityConfiguration + UserService

When tests use `@WebMvcTest` and `SecurityConfiguration` is auto-loaded (or explicitly imported),
Spring requires `UserService` as a bean because `SecurityConfiguration.jwtAuthenticationConverter(UserService)` has a `UserService` parameter.

**Fix:** Add `@MockitoBean private UserService userService;` to all affected `@WebMvcTest` test classes.

Affected test files found in this project:
- All controller tests that use `@WebMvcTest`
- The `SecurityConfiguration` `jwtAuthenticationConverter` bean method signature: `public Converter<Jwt, JwtAuthenticationToken> jwtAuthenticationConverter(UserService userService)`

## Service Impl Visibility

`ManagementServiceImpl` and `RegistrationServiceImpl` in `members.management` package are `public class`
(made public to fix Spring bean injection across test module boundaries).

## Package Structure

- Domain: `com.klabis.members.domain` — pure domain, no Spring
- Infrastructure: `com.klabis.members.infrastructure.restapi` — controllers
- Infrastructure: `com.klabis.members.infrastructure.jdbc` — repositories
- Management: `com.klabis.members.application` — application services

## MemberId / UserId Relationship

`MemberId.uuid()` == `UserId.uuid()` for all members (shared identity).
`MemberId.toUserId()` and `MemberId.fromUserId()` conversions available.

## Test Infrastructure

- Always use `test-runner` skill — never run `./gradlew test` directly
- For `@WebMvcTest` tests: mock `UserService`, `UserDetailsService`, and service layer beans
- `@DataJdbcTest` for repository tests — use `@ComponentScan.Filter(type=FilterType.ANNOTATION, value=Repository.class)`
