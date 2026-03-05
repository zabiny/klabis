package com.klabis.common.users;

import com.klabis.common.users.domain.UserPermissions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link UserPermissions} aggregate.
 * <p>
 * Tests business rules and invariants:
 * - Factory methods (create, empty)
 * - Authority management (grant, revoke, has)
 * - Immutability and idempotency
 * - Equality and hashCode
 */
class UserPermissionsTest {

    private static final UserId TEST_USER_ID = new UserId(UUID.randomUUID());

    @Test
    void shouldCreateUserPermissionsWithAuthorities() {
        // Given
        Set<Authority> authorities = Set.of(
                Authority.MEMBERS_READ,
                Authority.MEMBERS_CREATE
        );

        // When
        UserPermissions permissions = UserPermissions.create(TEST_USER_ID, authorities);

        // Then
        assertThat(permissions.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(permissions.getDirectAuthorities()).containsExactlyInAnyOrder(
                Authority.MEMBERS_READ,
                Authority.MEMBERS_CREATE
        );
    }

    @Test
    void shouldCreateUserPermissionsEmpty() {
        // When
        UserPermissions permissions = UserPermissions.empty(TEST_USER_ID);

        // Then
        assertThat(permissions.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(permissions.getDirectAuthorities()).isEmpty();
    }

    @Test
    void shouldFailToCreateWithNullUserId() {
        // Given
        Set<Authority> authorities = Set.of(Authority.MEMBERS_READ);

        // When/Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UserPermissions.create(null, authorities))
                .withMessageContaining("UserId must not be null");
    }

    @Test
    void shouldFailToCreateWithNullAuthorities() {
        // When/Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UserPermissions.create(TEST_USER_ID, null))
                .withMessageContaining("Direct authorities must not be null");
    }

    @Test
    void shouldCheckHasDirectAuthority() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                TEST_USER_ID,
                Set.of(Authority.MEMBERS_READ, Authority.MEMBERS_CREATE)
        );

        // When/Then
        assertThat(permissions.hasDirectAuthority(Authority.MEMBERS_READ)).isTrue();
        assertThat(permissions.hasDirectAuthority(Authority.MEMBERS_CREATE)).isTrue();
        assertThat(permissions.hasDirectAuthority(Authority.MEMBERS_DELETE)).isFalse();
    }

    @Test
    void shouldGrantAuthority() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                TEST_USER_ID,
                Set.of(Authority.MEMBERS_READ)
        );

        // When
        permissions.grantAuthority(Authority.MEMBERS_CREATE);

        // Then
        assertThat(permissions.getDirectAuthorities()).containsExactlyInAnyOrder(
                Authority.MEMBERS_READ,
                Authority.MEMBERS_CREATE
        );
    }

    @Test
    void shouldGrantAuthorityIdempotently() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                TEST_USER_ID,
                Set.of(Authority.MEMBERS_READ)
        );

        // When - grant same authority twice
        permissions.grantAuthority(Authority.MEMBERS_READ);
        permissions.grantAuthority(Authority.MEMBERS_READ);

        // Then - no duplicate authorities
        assertThat(permissions.getDirectAuthorities()).containsExactly(Authority.MEMBERS_READ);
    }

    @Test
    void shouldFailToGrantNullAuthority() {
        // Given
        UserPermissions permissions = UserPermissions.empty(TEST_USER_ID);

        // When/Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> permissions.grantAuthority((Authority) null))
                .withMessageContaining("Authority must not be null");
    }

    @Test
    void shouldRevokeAuthority() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                TEST_USER_ID,
                Set.of(Authority.MEMBERS_READ, Authority.MEMBERS_CREATE)
        );

        // When
        permissions.revokeAuthority(Authority.MEMBERS_CREATE);

        // Then
        assertThat(permissions.getDirectAuthorities()).containsExactly(Authority.MEMBERS_READ);
    }

    @Test
    void shouldRevokeAuthorityIdempotently() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                TEST_USER_ID,
                Set.of(Authority.MEMBERS_READ)
        );

        // When - revoke same authority twice
        permissions.revokeAuthority(Authority.MEMBERS_CREATE);
        permissions.revokeAuthority(Authority.MEMBERS_CREATE);

        // Then - no error, authorities unchanged
        assertThat(permissions.getDirectAuthorities()).containsExactly(Authority.MEMBERS_READ);
    }

    @Test
    void shouldFailToRevokeNullAuthority() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                TEST_USER_ID,
                Set.of(Authority.MEMBERS_READ)
        );

        // When/Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> permissions.revokeAuthority((Authority) null))
                .withMessageContaining("Authority must not be null");
    }

    @Test
    void shouldReplaceAllAuthorities() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                TEST_USER_ID,
                Set.of(Authority.MEMBERS_READ, Authority.MEMBERS_CREATE)
        );

        // When
        Set<Authority> newAuthorities = Set.of(
                Authority.MEMBERS_UPDATE,
                Authority.MEMBERS_DELETE
        );
        permissions.replaceAuthorities(newAuthorities);

        // Then
        assertThat(permissions.getDirectAuthorities()).containsExactlyInAnyOrder(
                Authority.MEMBERS_UPDATE,
                Authority.MEMBERS_DELETE
        );
    }

    @Test
    void shouldReplaceWithEmptyAuthorities() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                TEST_USER_ID,
                Set.of(Authority.MEMBERS_READ)
        );

        // When
        permissions.replaceAuthorities(Set.of());

        // Then
        assertThat(permissions.getDirectAuthorities()).isEmpty();
    }

    @Test
    void shouldFailToReplaceWithNullAuthorities() {
        // Given
        UserPermissions permissions = UserPermissions.empty(TEST_USER_ID);

        // When/Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> permissions.replaceAuthorities((Set<Authority>) null))
                .withMessageContaining("New authorities must not be null");
    }

    @Test
    void shouldReturnUnmodifiableAuthoritiesSet() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                TEST_USER_ID,
                Set.of(Authority.MEMBERS_READ)
        );

        // When
        Set<Authority> authorities = permissions.getDirectAuthorities();

        // Then - modifying returned set should throw exception
        assertThatThrownBy(() -> authorities.add(Authority.MEMBERS_CREATE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldImplementEqualityBasedOnUserId() {
        // Given
        UserId sameUserId = TEST_USER_ID;
        UserId differentUserId = new UserId(UUID.randomUUID());

        UserPermissions permissions1 = UserPermissions.create(sameUserId, Set.of(Authority.MEMBERS_READ));
        UserPermissions permissions2 = UserPermissions.create(sameUserId, Set.of(Authority.MEMBERS_CREATE));
        UserPermissions permissions3 = UserPermissions.create(differentUserId, Set.of(Authority.MEMBERS_READ));

        // Then
        assertThat(permissions1).isEqualTo(permissions2);
        assertThat(permissions1).isNotEqualTo(permissions3);
        assertThat(permissions1.hashCode()).isEqualTo(permissions2.hashCode());
    }

    @Test
    void shouldHaveReadableToString() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                TEST_USER_ID,
                Set.of(Authority.MEMBERS_READ, Authority.MEMBERS_CREATE)
        );

        // When
        String toString = permissions.toString();

        // Then
        assertThat(toString).contains("UserPermissions");
        assertThat(toString).contains("UserId[uuid=" + TEST_USER_ID.uuid() + "]");
        assertThat(toString).contains("directAuthorities=");
    }
}
