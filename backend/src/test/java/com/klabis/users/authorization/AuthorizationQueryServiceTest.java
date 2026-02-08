package com.klabis.users.authorization;

import com.klabis.users.Authority;
import com.klabis.users.UserId;
import com.klabis.users.UserPermissions;
import com.klabis.users.persistence.UserPermissionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthorizationQueryService}.
 * <p>
 * Tests authorization query logic:
 * - Direct authority checking
 * - Missing permissions handling
 * - Contextual authorization
 */
@ExtendWith(MockitoExtension.class)
class AuthorizationQueryServiceTest {

    @Mock
    private UserPermissionsRepository permissionsRepo;

    private AuthorizationQueryService service;

    private final UserId testUserId = new UserId(UUID.randomUUID());
    private final UserId otherUserId = new UserId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        service = new AuthorizationQueryServiceImpl(permissionsRepo);
    }

    @Test
    void shouldReturnTrueWhenUserHasRequiredAuthority() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                testUserId,
                Set.of(Authority.MEMBERS_READ, Authority.MEMBERS_CREATE)
        );
        when(permissionsRepo.findById(testUserId)).thenReturn(Optional.of(permissions));

        AuthorizationContext context = new AuthorizationContext(
                testUserId,
                otherUserId,
                Authority.MEMBERS_READ
        );

        // When
        boolean authorized = service.checkAuthorization(context);

        // Then
        assertThat(authorized).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotHaveRequiredAuthority() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                testUserId,
                Set.of(Authority.MEMBERS_READ) // Does not have MEMBERS_DELETE
        );
        when(permissionsRepo.findById(testUserId)).thenReturn(Optional.of(permissions));

        AuthorizationContext context = new AuthorizationContext(
                testUserId,
                otherUserId,
                Authority.MEMBERS_DELETE
        );

        // When
        boolean authorized = service.checkAuthorization(context);

        // Then
        assertThat(authorized).isFalse();
    }

    @Test
    void shouldReturnFalseWhenUserHasNoPermissionsRecord() {
        // Given
        when(permissionsRepo.findById(testUserId)).thenReturn(Optional.empty());

        AuthorizationContext context = new AuthorizationContext(
                testUserId,
                otherUserId,
                Authority.MEMBERS_READ
        );

        // When
        boolean authorized = service.checkAuthorization(context);

        // Then
        assertThat(authorized).isFalse();
    }

    @Test
    void shouldReturnTrueForSelfAccessWithAuthority() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                testUserId,
                Set.of(Authority.MEMBERS_READ)
        );
        when(permissionsRepo.findById(testUserId)).thenReturn(Optional.of(permissions));

        // Self-access context (actor = resourceOwner)
        AuthorizationContext context = AuthorizationContext.selfAccess(
                testUserId,
                Authority.MEMBERS_READ
        );

        // When
        boolean authorized = service.checkAuthorization(context);

        // Then
        assertThat(authorized).isTrue();
    }

    @Test
    void shouldReturnFalseForSelfAccessWithoutAuthority() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                testUserId,
                Set.of() // No authorities
        );
        when(permissionsRepo.findById(testUserId)).thenReturn(Optional.of(permissions));

        // Self-access context (actor = resourceOwner)
        AuthorizationContext context = AuthorizationContext.selfAccess(
                testUserId,
                Authority.MEMBERS_READ
        );

        // When
        boolean authorized = service.checkAuthorization(context);

        // Then
        assertThat(authorized).isFalse();
    }

    @Test
    void shouldReturnTrueForHasAuthorityWithValidString() {
        // Given
        UserPermissions permissions = UserPermissions.create(
                testUserId,
                Set.of(Authority.MEMBERS_READ)
        );
        when(permissionsRepo.findById(testUserId)).thenReturn(Optional.of(permissions));

        // When
        boolean hasAuthority = service.hasAuthority(testUserId, "MEMBERS:READ");

        // Then
        assertThat(hasAuthority).isTrue();
    }

    @Test
    void shouldReturnFalseForHasAuthorityWithInvalidString() {
        // Given - no stubbing needed since invalid authority is caught before repository call

        // When
        boolean hasAuthority = service.hasAuthority(testUserId, "INVALID:AUTHORITY");

        // Then
        assertThat(hasAuthority).isFalse();
    }

    @Test
    void shouldReturnFalseForHasAuthorityWhenNoPermissions() {
        // Given
        when(permissionsRepo.findById(testUserId)).thenReturn(Optional.empty());

        // When
        boolean hasAuthority = service.hasAuthority(testUserId, "MEMBERS:READ");

        // Then
        assertThat(hasAuthority).isFalse();
    }
}
