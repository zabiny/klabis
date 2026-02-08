# Change: Add Email Service for Member Registration

## Why

New members need to receive a welcome email with an account activation link when they register. The `MemberCreatedEvent`
domain event is already implemented and published, but no event handler exists to send emails. This creates a poor user
experience where members register but receive no confirmation or way to activate their accounts.

## What Changes

- **Add email service infrastructure** using Spring's JavaMailSender with existing SMTP configuration
- **Add welcome email templates** with both HTML and plain-text versions using Thymeleaf
- **Add activation token generation** for secure email-based account activation
- **Add MemberCreatedEventHandler** that listens to domain events and triggers welcome emails
- **Add email sending with transactional safety** using `@TransactionalEventListener(AFTER_COMMIT)`
- **Add email test infrastructure** for integration testing of email functionality

## Impact

### Affected Specs

- NEW: `email-service` capability (email sending, templates, activation tokens)

### Affected Code

- `com.klabis.common.email` - Shared email infrastructure (EmailService, templates)
- `com.klabis.members.application` - MemberCreatedEventHandler (uses EmailService)
- `com.klabis.users.domain` - Activation token storage
- `src/main/resources/templates/email/` - Thymeleaf email templates
- `application.yml` - Email configuration properties
- `pom.xml` - Thymeleaf dependency

### Dependencies

- Requires: `MemberCreatedEvent` (already implemented)
- Requires: SMTP server configuration (already in application.yml)
- Requires: User entity updates for activation tokens

### Migration

- No breaking changes
- New database column for activation tokens (Flyway migration)
- Existing members remain unaffected (activation only for new registrations)

## Scope Boundaries

### In Scope

- Welcome email on member registration
- Email-based account activation flow
- HTML + plain-text email templates
- Email integration testing

### Out of Scope (Deferred)

- Password reset emails (future proposal)
- Event notification emails (future proposal)
- Spring Modulith outbox pattern (documented in OUTBOX_PATTERN.md for future)
- Email delivery tracking/analytics
- Email rate limiting
- Bulk email sending

## Risk Assessment

| Risk                      | Likelihood | Impact | Mitigation                                               |
|---------------------------|------------|--------|----------------------------------------------------------|
| SMTP server unavailable   | Medium     | Medium | Graceful failure with logging, retry mechanism           |
| Email marked as spam      | Low        | Medium | Proper SPF/DKIM configuration (ops), plain-text fallback |
| Activation token security | Low        | High   | Secure random generation, expiration, single-use         |

## Success Criteria

1. New members receive welcome email within 5 seconds of registration
2. Activation links work correctly and activate user accounts
3. Email templates render correctly in major email clients
4. Integration tests verify email sending without external SMTP
5. Failed email delivery does not break member registration (async, after commit)
