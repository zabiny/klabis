package com.klabis.common.users;

import com.klabis.common.users.domain.AuthorizationPolicy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link AuthorizationPolicy} business rules.
 * <p>
 * Tests authorization business rules:
 * - Admin lockout prevention
 */
class AuthorizationPolicyTest {

    private static final UserId TEST_USER_ID = new UserId(UUID.randomUUID());

    @Test
    void shouldAllowRevokingNonMembersPermissionsAuthority() {
        // Given
        Authority nonAdminAuthority = Authority.MEMBERS_READ;
        long adminCount = 1; // Only one admin

        // When/Then - should not throw exception
        assertThatCode(() ->
                AuthorizationPolicy.checkAdminLockoutPrevention(TEST_USER_ID, nonAdminAuthority, adminCount)
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldPreventRevokingMembersPermissionsFromLastAdmin() {
        // Given
        Authority adminAuthority = Authority.MEMBERS_PERMISSIONS;
        long adminCount = 1; // Only one admin

        // When/Then
        assertThatThrownBy(() ->
                AuthorizationPolicy.checkAdminLockoutPrevention(TEST_USER_ID, adminAuthority, adminCount)
        )
                .isInstanceOf(AuthorizationPolicy.AdminLockoutException.class)
                .hasMessageContaining("Cannot revoke MEMBERS:PERMISSIONS")
                .hasMessageContaining("zero permission managers");
    }

    @Test
    void shouldAllowRevokingMembersPermissionsWhenMultipleAdminsExist() {
        // Given
        Authority adminAuthority = Authority.MEMBERS_PERMISSIONS;
        long adminCount = 2; // Multiple admins exist

        // When/Then - should not throw exception
        assertThatCode(() ->
                AuthorizationPolicy.checkAdminLockoutPrevention(TEST_USER_ID, adminAuthority, adminCount)
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldIncludeUserIdInAdminLockoutErrorMessage() {
        // Given
        Authority adminAuthority = Authority.MEMBERS_PERMISSIONS;
        long adminCount = 1;

        // When
        Throwable thrown = catchThrowable(() ->
                AuthorizationPolicy.checkAdminLockoutPrevention(TEST_USER_ID, adminAuthority, adminCount)
        );

        // Then
        assertThat(thrown)
                .isInstanceOf(AuthorizationPolicy.AdminLockoutException.class)
                .hasMessageContaining(TEST_USER_ID.uuid().toString());
    }
}
