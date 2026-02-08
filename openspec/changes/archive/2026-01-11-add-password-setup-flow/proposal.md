# Change: Add Password Setup Flow for User Activation

**Status: 🔄 PENDING IMPLEMENTATION - Replace Current Solution**

## Why

Currently, the member registration process creates user accounts with `PENDING_ACTIVATION` status. An interim
email-based activation flow exists, but it has critical security and usability issues:

- **Temporary passwords remain unused**: Users are activated but never receive their temporary passwords
- **Poor security model**: Temporary passwords are generated but never delivered, creating a false sense of security
- **No password self-service**: Users cannot set their own secure passwords during activation
- **Missing password reset requirement**: Users activated with temporary passwords have no mechanism to change them

This password setup flow proposal will replace the interim solution with a proper security model where users set their
own passwords during activation.

## What Changes

- **Add password setup token system** for secure, time-limited account activation
- **Create password setup email flow** to send activation links to new members
- **Implement token validation endpoints** for frontend password setup pages
- **Add password complexity validation** (min 12 chars, uppercase, lowercase, digit, special char)
- **Add token reissuance capability** for expired tokens with rate limiting
- **Implement audit logging** for all token-related security events
- **Update member registration flow** to trigger password setup email instead of generating temporary passwords

### Key Features

- Tokens expire after 4 hours (configurable)
- Single-use tokens (marked as used after password set)
- SHA-256 token hashing for security
- Rate limiting per registration number (3 requests/hour, 10 min min delay)
- Account automatically becomes ACTIVE after password set
- Email service using Spring Mail (SMTP)

## Impact

### Affected Specs

- **user-activation** (NEW): Complete password setup and activation capability
    - Token generation and management
    - Email notification system
    - Token validation and password setup
    - Token reissuance with rate limiting
    - Audit logging

### Affected Code

- `User` domain entity: Add factory method for creating users with pending activation
- `RegisterMemberCommandHandler`: Update to generate tokens and send emails instead of temporary passwords
- Database: New `password_setup_tokens` table (migration V004)
- Extended `com.klabis.users` package with:
    - `domain`: PasswordSetupToken aggregate root, TokenHash value object, PasswordSetupTokenRepository interface
    - `application`: PasswordSetupService (uses existing EmailService)
    - `infrastructure.persistence`: PasswordSetupTokenEntity, JPA repository, mapper
    - `infrastructure.jobs`: TokenCleanupJob
    - `presentation`: PasswordSetupController (REST API)
- Existing shared infrastructure:
    - `com.klabis.common.email.EmailService` (used directly, no modifications needed)
    - `com.klabis.common.audit.@Auditable` (used for audit logging, add new event types)

### Breaking Changes

- **NONE** - This is a new capability that enhances existing registration flow
- Existing member registration API remains unchanged (same request/response)
- Side effect changes only (different email behavior)

### Configuration Changes

- Add SMTP configuration properties (already scaffolded in application.yml)
- Add password-setup configuration block with token expiration and rate limits
- Add email template configuration

### Dependencies

- Spring Mail (already in dependencies)
- No new external dependencies required

---

## Current Implementation (Interim Solution)

### What Exists Now

An interim email-based activation flow was implemented as a temporary solution:

**Current Implementation:**

- **Token-based activation**: Users receive email with activation link
- **Simple activation process**: Click link → account activated → temporary password remains in system
- **Token storage**: Stored in `users` table columns (`activation_token`, `activation_token_expires_at`, `activated_at`)
- **No password setup**: Users activated but passwords never delivered
- **Migration**: `V003__add_activation_tokens.sql`
- **Domain**: `ActivationToken` value object in `com.klabis.users.domain`
- **Service**: `AccountActivationService` in `com.klabis.users.application`
- **Controller**: `AccountActivationController` with GET `/api/activate?token={token}`
- **Registration**: `RegisterMemberCommandHandler` creates users with temporary passwords and activation tokens

### Critical Issues with Current Implementation

1. **Security Gap**: Temporary passwords are generated but never sent to users
2. **No Password Delivery**: Users are activated but cannot log in (they don't know their password)
3. **Incomplete Flow**: Activation completes but account remains unusable
4. **Missing Password Reset**: No mechanism for users to set their own password after activation
5. **No Rate Limiting**: Token reissuance not protected against abuse
6. **No Token Cleanup**: Expired tokens accumulate in database

### Migration Strategy

Since the application is still in **development only** with no production deployment, the migration is straightforward:

**Step 1: Implement Password Setup Flow**

- Create new `password_setup_tokens` table (migration V004)
- Implement all password setup components
- Write comprehensive tests

**Step 2: Update Registration Flow**

- Modify `RegisterMemberCommandHandler` to use password setup tokens
- Update `User` domain with new factory methods
- `PasswordSetupService` will use existing `EmailService` for sending emails

**Step 3: Remove Old Activation Flow**

- Delete `ActivationToken`, `AccountActivationService`, `AccountActivationController`
- Remove activation token fields from `User` domain
- Update tests

**Step 4: Clean Up Database**

- Drop unused `activation_token` columns from `users` table (migration V005)

**Timeline**: 2-3 weeks (no complex phased approach needed for DEV)

### Implementation Comparison

| Feature             | Current (Interim)      | Proposed (Password Setup) |
|---------------------|------------------------|---------------------------|
| Password delivery   | ❌ Not sent             | ✅ User sets own password  |
| Account usability   | ❌ Cannot login         | ✅ Can login immediately   |
| Security model      | ⚠️ Temporary passwords | ✅ User-chosen passwords   |
| Token storage       | In `users` table       | Separate table            |
| Rate limiting       | ❌ None                 | ✅ Per registration number |
| Token reissuance    | ❌ Not available        | ✅ With rate limiting      |
| Password validation | ❌ None                 | ✅ Complexity rules        |
| Token cleanup       | ❌ Manual               | ✅ Scheduled job           |
| Audit logging       | ⚠️ Minimal             | ✅ Comprehensive           |

### Files to Create

- `V004__add_password_setup_tokens.sql` - New database migration
- `PasswordSetupToken` - Domain aggregate root
- `TokenHash` - Value object for secure token hashing
- `PasswordSetupTokenRepository` - Repository interface and implementation
- `PasswordSetupService` - Application service
- `PasswordComplexityValidator` - Password validation utility
- `RateLimitService` - Rate limiting service
- `PasswordSetupController` - REST API controller
- Email templates for password setup
- Scheduled job for token cleanup
- Comprehensive test suite

### Files to Modify

- `RegisterMemberCommandHandler` - Use password setup tokens instead of activation tokens
- `User` domain - Add `createPendingActivation()` and `activateWithPassword()` methods
- `AuditEventType` enum - Add password setup token event types
- `SecurityConfiguration` - Add public endpoints for password setup
- `application.yml` - Add password setup configuration

### Files to Deprecate/Remove

- `ActivationToken` value object (after transition)
- `AccountActivationService` (after transition)
- `AccountActivationController` (after transition)
- `activation_token` columns in `users` table (after transition)
