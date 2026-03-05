## Context

**Current State:**
- UserInfo endpoint (`/oauth2/userinfo`) returns custom claims: `firstName`, `lastName`, `registrationNumber`
- All claims returned under single `openid` scope (no granular control)
- Non-standard claim names violate OIDC specification
- Spring Security OAuth2 Authorization Server 1.5.5 with Spring Boot 3.5.9
- Member entity stores: PersonalInformation (firstName, lastName), EmailAddress value object, RegistrationNumber
- Admin users have no linked Member entity (only User record with username)

**Constraints:**
- Must maintain compatibility with OIDC specification (RFC 6749, OpenID Connect Core 1.0)
- Must handle cases where Member entity doesn't exist (admin users)
- Must handle cases where Member exists but email is null
- Cannot break existing OAuth2 authorization flow
- Must follow GDPR principle of data minimization

**Architecture:**
```
┌─────────────────────────────────────────────────────────┐
│  Client                                                  │
│  └─ Requests scopes: openid, profile, email            │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  AuthorizationServerConfiguration                       │
│  └─ oidcUserInfoMapper(Members members)                │
│     ├─ Extract authorized scopes from context           │
│     ├─ Load Member by registrationNumber                │
│     └─ Build OidcUserInfo with scope-filtered claims    │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│  OidcUserInfo Response                                   │
│  └─ Claims filtered by requested scopes                 │
└─────────────────────────────────────────────────────────┘
```

## Goals / Non-Goals

**Goals:**
- Implement OIDC-compliant scope-based access control for UserInfo endpoint
- Use standard OIDC claim names (given_name, family_name, email, email_verified)
- Return only claims permitted by authorized scopes
- Omit claims when underlying data is unavailable (null-safe)
- Maintain backward compatibility with existing authorization flow

**Non-Goals:**
- Email verification functionality (email_verified will always be false)
- Changes to token issuance flow (ID tokens, access tokens)
- Changes to consent screen or scope descriptions
- Migration of existing tokens (scope changes apply to new tokens only)
- Support for additional OIDC profile claims (phone_number, address, etc.)

## Decisions

### Decision 1: Scope-to-Claims Mapping

**Choice:** Implement strict scope-based claim filtering using OIDC standard scopes.

| Scope | Claims Returned | Condition |
|-------|----------------|-----------|
| `openid` | `sub` | Always (required) |
| `profile` | `given_name`, `family_name`, `registrationNumber`, `updated_at` | Member exists |
| `email` | `email`, `email_verified` | Member exists AND email not null |

**Rationale:**
- OIDC specification defines `profile` scope for profile claims (name, birthdate, etc.)
- OIDC specification defines `email` scope for email claims (email, email_verified)
- `registrationNumber` is custom claim but belongs to profile information
- Separating scopes allows clients to request only needed data (GDPR data minimization)

**Alternatives Considered:**
- ❌ Keep all claims under `openid` scope - violates OIDC spec, no granular control
- ❌ Create custom scopes (e.g., `user:profile`) - non-standard, confuses OIDC clients

### Decision 2: Claim Name Migration

**Choice:** Replace custom claims with OIDC standard claims, remove old claims entirely.

| Old Claim | New Claim | OIDC Standard |
|-----------|-----------|---------------|
| `firstName` | `given_name` | ✓ Yes |
| `lastName` | `family_name` | ✓ Yes |
| `registrationNumber` | `registrationNumber` | Custom (domain-specific) |
| N/A | `email` | ✓ Yes |
| N/A | `email_verified` | ✓ Yes |
| N/A | `updated_at` | ✓ Yes |

**Rationale:**
- OIDC clients expect standard claim names for interoperability
- No existing frontend consumes UserInfo endpoint (verified in proposal)
- Clean break prevents confusion between old/new claim names
- `registrationNumber` kept as custom claim (no OIDC equivalent for domain-specific ID)

**Alternatives Considered:**
- ❌ Keep both old and new claims (backward compatibility) - bloats response, no consumers to preserve
- ❌ Map firstName/lastName to given_name/family_name in clients - pushes complexity to consumers

### Decision 3: Null Handling Strategy

**Choice:** Omit claims entirely when data is unavailable (OIDC best practice).

**Scenarios:**

```java
// Scenario A: Admin user (no Member entity)
{
  "sub": "admin"
  // No profile claims, no email claims
}

// Scenario B: Member without email
{
  "sub": "ZBM0501",
  "given_name": "Jan",
  "family_name": "Novák",
  "registrationNumber": "ZBM0501"
  // No email claims
}

// Scenario C: Member with email
{
  "sub": "ZBM0501",
  "given_name": "Jan",
  "family_name": "Novák",
  "registrationNumber": "ZBM0501",
  "email": "jan.novak@example.com",
  "email_verified": false
}
```

**Rationale:**
- OIDC specification allows omitting optional claims when unavailable
- Cleaner than returning `null` values
- Client can check claim presence with `hasOwnProperty()` or similar
- Prevents null pointer exceptions in clients expecting string values

**Alternatives Considered:**
- ❌ Return `null` for missing claims - forces clients to handle null, not OIDC idiomatic
- ❌ Return empty string `""` - semantically incorrect, client can't distinguish missing vs. empty

### Decision 4: Email Verification Status

**Choice:** Always return `email_verified: false` until email verification feature is implemented.

**Rationale:**
- OIDC spec requires `email_verified` claim when `email` claim is present
- Honest representation of current state (emails are not verified)
- Prevents security issues from clients trusting unverified emails
- Future-proof for when email verification is added

**Alternatives Considered:**
- ❌ Omit `email_verified` claim - violates OIDC spec when email present
- ❌ Return `email_verified: true` - dishonest, security risk

### Decision 5: Implementation Approach

**Choice:** Modify existing `oidcUserInfoMapper()` function in `AuthorizationServerConfiguration`.

**Implementation Pattern:**
```java
private Function<OidcUserInfoAuthenticationContext, OidcUserInfo> oidcUserInfoMapper(
        Members members) {
    return context -> {
        // 1. Get authorized scopes from context
        Set<String> scopes = context.getAuthorization().getAuthorizedScopes();

        // 2. Extract subject (always present)
        String subject = context.getAuthentication().getPrincipal()...getSubject();
        OidcUserInfo.Builder builder = OidcUserInfo.builder().subject(subject);

        // 3. Load Member only if scopes require it (optimization)
        if (scopes.contains("profile") || scopes.contains("email")) {
            if (RegistrationNumber.isRegistrationNumber(subject)) {
                members.findByRegistrationNumber(RegistrationNumber.of(subject))
                    .ifPresent(member -> {
                        addProfileClaims(builder, scopes, member, subject);
                        addEmailClaims(builder, scopes, member);
                    });
            }
        }

        return builder.build();
    };
}
```

**Rationale:**
- Single responsibility: UserInfo mapping lives in one place
- Uses existing Spring Security OAuth2 extension point
- Leverages Optional pattern for null-safe Member lookup
- Scope check before database query (performance optimization)

**Alternatives Considered:**
- ❌ Custom UserInfo endpoint controller - duplicates Spring Security OAuth2 logic
- ❌ Post-processing filter - more complex, harder to test

## Risks / Trade-offs

### Risk 1: Breaking Change for Future Frontend
**Risk:** If frontend is added before this change is communicated, it may expect old claim names (`firstName`, `lastName`).

**Mitigation:**
- Document breaking change clearly in proposal
- Update all specs and documentation with new claim names
- No frontend currently exists (verified)
- When frontend is developed, it will use correct claims from start

### Risk 2: Email Verification Confusion
**Risk:** Clients may ignore `email_verified: false` and trust unverified emails.

**Mitigation:**
- Always return `email_verified: false` (honest representation)
- Document in specs that emails are not currently verified
- When verification is added, update to `true` for verified emails

### Risk 3: Performance Impact
**Risk:** Loading Member entity on every UserInfo request could impact performance.

**Mitigation:**
- Only query Member if `profile` or `email` scope present (early exit for `openid`-only)
- Member lookup is by registrationNumber (indexed in database)
- UserInfo endpoint typically called once per session (caching at client)
- Consider adding caching layer if performance becomes issue (future optimization)

**Trade-off:** Slight performance overhead vs. data freshness (always latest data)

### Risk 4: Scope Creep - Additional Profile Claims
**Risk:** Future requests for more OIDC profile claims (phone_number, address, birthdate, etc.).

**Mitigation:**
- Clearly document Non-Goals (only given_name, family_name, email for now)
- Design is extensible - adding claims follows same pattern
- Member entity already has phone, address (infrastructure ready)

**Trade-off:** Minimal implementation now vs. complete OIDC profile support

## Migration Plan

**Deployment Steps:**
1. Deploy code with new scope-aware mapper and scopes in BootstrapDataLoader
2. Existing OAuth2 clients will continue to work (backward compatible at endpoint level)
3. Clients requesting new scopes (`profile`, `email`) will receive standard claims
4. No database migration needed (reads existing Member data)

**Rollback Strategy:**
- Revert code changes (single commit)
- No data migration to roll back
- No database schema changes

**Testing Strategy:**
- Unit tests for scope filtering logic
- Integration tests for all scope combinations
- E2E test with real Member entity (with and without email)
- Test admin user scenario (no Member entity)

**Monitoring:**
- Log UserInfo requests with requested scopes (detect usage patterns)
- Monitor error rate at `/oauth2/userinfo` endpoint
- Track which scopes clients are requesting (analytics)

## Open Questions

**Q1:** Should `registrationNumber` move to a custom scope (e.g., `klabis:registration`) instead of `profile`?
- **Leaning:** No - keep it in `profile` for simplicity. It's profile-related information.
- **Decision:** Keep in `profile` scope (domain-specific but profile-related)

**Q2:** When email verification is implemented, how to mark emails as verified?
- **Future work:** Add `email_verified` boolean to Member entity or EmailAddress value object
- **Not blocking:** Can return `false` for now

**Q3:** Should we add `updated_at` claim (OIDC standard for profile last update timestamp)?
- **Decision:** Yes - add `updated_at` claim to profile scope. Member entity already tracks `lastModifiedAt` via AuditMetadata, so data is available. This is an OIDC standard claim that helps clients detect stale cached profile data.
