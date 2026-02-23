package com.klabis.common.users;

import org.assertj.core.api.AbstractAssert;

import java.time.Instant;

/**
 * Custom AssertJ assertions for User aggregate root testing.
 * <p>
 * Provides fluent assertion methods for verifying User state, account status,
 * authentication capabilities, and audit metadata.
 * <p>
 * Usage example:
 * <pre>
 * UserAssert.assertThat(user)
 *     .hasUsername("ZBM9001")
 *     .hasAccountStatus(AccountStatus.ACTIVE)
 *     .isEnabled()
 *     .isAuthenticatable();
 * </pre>
 */
public class UserAssert extends AbstractAssert<UserAssert, User> {

    private UserAssert(User actual) {
        super(actual, UserAssert.class);
    }

    public static UserAssert assertThat(User actual) {
        return new UserAssert(actual);
    }

    // ===== Identity Assertions =====

    public UserAssert hasId(UserId expected) {
        isNotNull();
        if (!actual.getId().equals(expected)) {
            failWithMessage("Expected user ID to be <%s> but was <%s>", expected, actual.getId());
        }
        return this;
    }

    public UserAssert hasIdNotNull() {
        isNotNull();
        if (actual.getId() == null) {
            failWithMessage("Expected user ID to be not null");
        }
        return this;
    }

    public UserAssert hasUsername(String expected) {
        isNotNull();
        if (!actual.getUsername().equals(expected)) {
            failWithMessage("Expected username to be <%s> but was <%s>", expected, actual.getUsername());
        }
        return this;
    }

    public UserAssert hasPasswordHash(String expected) {
        isNotNull();
        if (!actual.getPasswordHash().equals(expected)) {
            failWithMessage("Expected password hash to be <%s> but was <%s>", expected, actual.getPasswordHash());
        }
        return this;
    }

    // ===== Account Status Assertions =====

    public UserAssert hasAccountStatus(AccountStatus expected) {
        isNotNull();
        if (actual.getAccountStatus() != expected) {
            failWithMessage("Expected account status to be <%s> but was <%s>", expected, actual.getAccountStatus());
        }
        return this;
    }

    public UserAssert isEnabled() {
        isNotNull();
        if (!actual.isEnabled()) {
            failWithMessage("Expected user to be enabled");
        }
        return this;
    }

    public UserAssert isNotEnabled() {
        isNotNull();
        if (actual.isEnabled()) {
            failWithMessage("Expected user to be not enabled");
        }
        return this;
    }

    public UserAssert isAuthenticatable() {
        isNotNull();
        if (!actual.isAuthenticatable()) {
            failWithMessage("Expected user to be authenticatable");
        }
        return this;
    }

    public UserAssert isNotAuthenticatable() {
        isNotNull();
        if (actual.isAuthenticatable()) {
            failWithMessage("Expected user to be not authenticatable");
        }
        return this;
    }

    // ===== Spring Security Flags =====

    public UserAssert isAccountNonExpired() {
        isNotNull();
        if (!actual.isAccountNonExpired()) {
            failWithMessage("Expected account to be non-expired");
        }
        return this;
    }

    public UserAssert isAccountExpired() {
        isNotNull();
        if (actual.isAccountNonExpired()) {
            failWithMessage("Expected account to be expired");
        }
        return this;
    }

    public UserAssert isAccountNonLocked() {
        isNotNull();
        if (!actual.isAccountNonLocked()) {
            failWithMessage("Expected account to be non-locked");
        }
        return this;
    }

    public UserAssert isAccountLocked() {
        isNotNull();
        if (actual.isAccountNonLocked()) {
            failWithMessage("Expected account to be locked");
        }
        return this;
    }

    public UserAssert isCredentialsNonExpired() {
        isNotNull();
        if (!actual.isCredentialsNonExpired()) {
            failWithMessage("Expected credentials to be non-expired");
        }
        return this;
    }

    public UserAssert isCredentialsExpired() {
        isNotNull();
        if (actual.isCredentialsNonExpired()) {
            failWithMessage("Expected credentials to be expired");
        }
        return this;
    }

    // ===== Audit Metadata Assertions =====

    public UserAssert hasCreatedAtNotNull() {
        isNotNull();
        if (actual.getCreatedAt() == null) {
            failWithMessage("Expected createdAt to be not null");
        }
        return this;
    }

    public UserAssert hasCreatedAt(Instant expected) {
        isNotNull();
        if (!actual.getCreatedAt().equals(expected)) {
            failWithMessage("Expected createdAt to be <%s> but was <%s>", expected, actual.getCreatedAt());
        }
        return this;
    }

    public UserAssert hasLastModifiedAtNotNull() {
        isNotNull();
        if (actual.getLastModifiedAt() == null) {
            failWithMessage("Expected lastModifiedAt to be not null");
        }
        return this;
    }

    public UserAssert hasLastModifiedAtAfter(Instant reference) {
        isNotNull();
        if (actual.getLastModifiedAt() == null) {
            failWithMessage("Expected lastModifiedAt to be not null");
        }
        if (!actual.getLastModifiedAt().isAfter(reference)) {
            failWithMessage("Expected lastModifiedAt <%s> to be after <%s>", actual.getLastModifiedAt(), reference);
        }
        return this;
    }

    public UserAssert hasVersion(Long expected) {
        isNotNull();
        if (!actual.getVersion().equals(expected)) {
            failWithMessage("Expected version to be <%s> but was <%s>", expected, actual.getVersion());
        }
        return this;
    }

    public UserAssert hasVersionNotNull() {
        isNotNull();
        if (actual.getVersion() == null) {
            failWithMessage("Expected version to be not null");
        }
        return this;
    }

    public UserAssert hasVersionGreaterThan(Long reference) {
        isNotNull();
        if (actual.getVersion() == null) {
            failWithMessage("Expected version to be not null");
        }
        if (actual.getVersion() <= reference) {
            failWithMessage("Expected version <%s> to be greater than <%s>", actual.getVersion(), reference);
        }
        return this;
    }

    // ===== High-Level State Assertions =====

    /**
     * Verifies that the user is in ACTIVE state (active account + enabled + authenticatable).
     */
    public UserAssert isActiveUser() {
        isNotNull();
        hasAccountStatus(AccountStatus.ACTIVE);
        isEnabled();
        isAuthenticatable();
        return this;
    }

    /**
     * Verifies that the user is in PENDING_ACTIVATION state (pending + not enabled + not authenticatable).
     */
    public UserAssert isPendingActivationUser() {
        isNotNull();
        hasAccountStatus(AccountStatus.PENDING_ACTIVATION);
        isNotEnabled();
        isNotAuthenticatable();
        return this;
    }

    /**
     * Verifies that the user has same identity (ID and username) as the expected user.
     */
    public UserAssert hasSameIdentityAs(User expected) {
        isNotNull();
        if (expected == null) {
            failWithMessage("Expected user is null");
        }
        hasId(expected.getId());
        hasUsername(expected.getUsername());
        return this;
    }
}