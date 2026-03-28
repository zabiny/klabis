package com.klabis.common.users.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.AuthorizationPolicy;
import com.klabis.common.users.domain.UserPermissions;
import com.klabis.common.users.domain.UserPermissionsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Unit Tests")
class PermissionServiceTest {

    private static final UserId USER_ID = new UserId(UUID.randomUUID());

    @Mock
    private UserPermissionsRepository permissionsRepository;

    private PermissionService service;

    @BeforeEach
    void setUp() {
        service = new PermissionServiceImpl(permissionsRepository);
    }

    @Nested
    @DisplayName("updateUserPermissions() method")
    class UpdateUserPermissionsMethod {

        @Test
        @DisplayName("should update user authorities with valid authorities and always persist standard authorities")
        void shouldUpdateUserAuthoritiesWithValidAuthorities() {
            // Given
            UserPermissions existingPermissions = UserPermissions.create(
                    USER_ID,
                    Set.of(Authority.MEMBERS_READ, Authority.EVENTS_READ)
            );

            when(permissionsRepository.findById(USER_ID)).thenReturn(Optional.of(existingPermissions));
            when(permissionsRepository.save(any(UserPermissions.class))).thenAnswer(invocation -> invocation.getArgument(
                    0));

            // When
            UserPermissions updatedPermissions = service.updateUserPermissions(
                    USER_ID,
                    Set.of(Authority.MEMBERS_MANAGE)
            );

            // Then - standard authorities are always persisted regardless of input
            assertThat(updatedPermissions.getDirectAuthorities())
                    .contains(Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ, Authority.EVENTS_READ);
            verify(permissionsRepository).save(argThat(perms ->
                    perms.getDirectAuthorities().containsAll(
                            Set.of(Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ, Authority.EVENTS_READ)) &&
                    perms.getUserId().equals(USER_ID)
            ));
        }

        @Test
        @DisplayName("should always persist standard authorities even when not included in request")
        void shouldAlwaysPersistStandardAuthoritiesEvenWhenNotInRequest() {
            // Given - request does not include MEMBERS_READ or EVENTS_READ
            when(permissionsRepository.findById(USER_ID)).thenReturn(Optional.empty());
            when(permissionsRepository.save(any(UserPermissions.class))).thenAnswer(invocation -> invocation.getArgument(
                    0));

            // When
            UserPermissions updatedPermissions = service.updateUserPermissions(
                    USER_ID,
                    Set.of(Authority.MEMBERS_MANAGE)
            );

            // Then - standard authorities must be present in persisted data
            assertThat(updatedPermissions.getDirectAuthorities())
                    .containsAll(Authority.getStandardUserAuthorities());
            verify(permissionsRepository).save(argThat(perms ->
                    perms.getDirectAuthorities().containsAll(Authority.getStandardUserAuthorities())
            ));
        }

        @Test
        @DisplayName("should create new permissions when user has no existing permissions and merge standard authorities")
        void shouldCreateNewPermissionsWhenUserHasNoExistingPermissions() {
            // Given
            when(permissionsRepository.findById(USER_ID)).thenReturn(Optional.empty());
            when(permissionsRepository.save(any(UserPermissions.class))).thenAnswer(invocation -> invocation.getArgument(
                    0));

            // When
            UserPermissions updatedPermissions = service.updateUserPermissions(
                    USER_ID,
                    Set.of(Authority.MEMBERS_MANAGE)
            );

            // Then - standard authorities merged in
            assertThat(updatedPermissions.getDirectAuthorities())
                    .contains(Authority.MEMBERS_MANAGE)
                    .containsAll(Authority.getStandardUserAuthorities());
            verify(permissionsRepository).save(argThat(perms ->
                    perms.getDirectAuthorities().contains(Authority.MEMBERS_MANAGE) &&
                    perms.getDirectAuthorities().containsAll(Authority.getStandardUserAuthorities()) &&
                    perms.getUserId().equals(USER_ID)
            ));
        }

        @Test
        @DisplayName("should throw AuthorizationPolicy.AdminLockoutException when removing MEMBERS:PERMISSIONS from last admin")
        void shouldThrowExceptionWhenRemovingLastAdmin() {
            // Given
            UserPermissions adminPermissions = UserPermissions.create(
                    USER_ID,
                    Set.of(Authority.MEMBERS_PERMISSIONS, Authority.MEMBERS_MANAGE)
            );

            when(permissionsRepository.findById(USER_ID)).thenReturn(Optional.of(adminPermissions));
            when(permissionsRepository.countUsersWithAuthority(Authority.MEMBERS_PERMISSIONS)).thenReturn(1L); // Only one admin

            // When & Then
            assertThatThrownBy(() -> service.updateUserPermissions(
                    USER_ID,
                    Set.of(Authority.MEMBERS_MANAGE)
            ))
                    .isInstanceOf(AuthorizationPolicy.AdminLockoutException.class)
                    .hasMessageContaining("Cannot revoke MEMBERS:PERMISSIONS from user");

            verify(permissionsRepository, never()).save(any(UserPermissions.class));
        }

        @Test
        @DisplayName("should allow removing MEMBERS:PERMISSIONS when other admins exist")
        void shouldAllowRemovingAdminPermissionsWhenOtherAdminsExist() {
            // Given
            UserPermissions adminPermissions = UserPermissions.create(
                    USER_ID,
                    Set.of(Authority.MEMBERS_PERMISSIONS, Authority.MEMBERS_MANAGE)
            );

            when(permissionsRepository.findById(USER_ID)).thenReturn(Optional.of(adminPermissions));
            when(permissionsRepository.countUsersWithAuthority(Authority.MEMBERS_PERMISSIONS)).thenReturn(2L); // Another admin exists
            when(permissionsRepository.save(any(UserPermissions.class))).thenAnswer(invocation -> invocation.getArgument(
                    0));

            // When
            UserPermissions updatedPermissions = service.updateUserPermissions(
                    USER_ID,
                    Set.of(Authority.MEMBERS_MANAGE)
            );

            // Then
            assertThat(updatedPermissions.getDirectAuthorities())
                    .contains(Authority.MEMBERS_MANAGE)
                    .doesNotContain(Authority.MEMBERS_PERMISSIONS);
            verify(permissionsRepository).save(argThat(perms ->
                    perms.getDirectAuthorities().contains(Authority.MEMBERS_MANAGE) &&
                    !perms.getDirectAuthorities().contains(Authority.MEMBERS_PERMISSIONS) &&
                    perms.getUserId().equals(USER_ID)
            ));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException for empty authorities")
        void shouldThrowExceptionForEmptyAuthorities() {
            // When & Then - validation happens before database lookup
            assertThatThrownBy(() -> service.updateUserPermissions(USER_ID, Set.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("At least one authority required");

            verify(permissionsRepository, never()).findById(any(UserId.class));
            verify(permissionsRepository, never()).save(any(UserPermissions.class));
        }

        @Test
        @DisplayName("should not check admin count when not removing MEMBERS:PERMISSIONS")
        void shouldNotCheckAdminCountWhenNotRemovingPermissionsAuthority() {
            // Given
            UserPermissions adminPermissions = UserPermissions.create(
                    USER_ID,
                    Set.of(Authority.MEMBERS_PERMISSIONS, Authority.MEMBERS_READ)
            );

            when(permissionsRepository.findById(USER_ID)).thenReturn(Optional.of(adminPermissions));
            when(permissionsRepository.save(any(UserPermissions.class))).thenAnswer(invocation -> invocation.getArgument(
                    0));

            // When
            UserPermissions updatedPermissions = service.updateUserPermissions(
                    USER_ID,
                    Set.of(Authority.MEMBERS_PERMISSIONS, Authority.MEMBERS_MANAGE)
            );

            // Then
            verify(permissionsRepository, never()).countUsersWithAuthority(any(Authority.class));
            assertThat(updatedPermissions.getDirectAuthorities())
                    .contains(Authority.MEMBERS_PERMISSIONS, Authority.MEMBERS_MANAGE);
            verify(permissionsRepository).save(argThat(perms ->
                    perms.getDirectAuthorities()
                            .containsAll(Set.of(Authority.MEMBERS_PERMISSIONS, Authority.MEMBERS_MANAGE)) &&
                    perms.getUserId().equals(USER_ID)
            ));
        }

        @Test
        @DisplayName("should preserve user ID when updating authorities")
        void shouldPreserveUserIdWhenUpdatingAuthorities() {
            // Given
            UserPermissions existingPermissions = UserPermissions.create(
                    USER_ID,
                    Set.of(Authority.MEMBERS_READ)
            );

            when(permissionsRepository.findById(USER_ID)).thenReturn(Optional.of(existingPermissions));
            when(permissionsRepository.save(any(UserPermissions.class))).thenAnswer(invocation -> invocation.getArgument(
                    0));

            // When
            UserPermissions updatedPermissions = service.updateUserPermissions(
                    USER_ID,
                    Set.of(Authority.MEMBERS_MANAGE)
            );

            // Then
            assertThat(updatedPermissions.getUserId()).isEqualTo(USER_ID);
            verify(permissionsRepository).save(argThat(perms ->
                    perms.getUserId().equals(USER_ID) &&
                    perms.getDirectAuthorities().contains(Authority.MEMBERS_MANAGE)
            ));
        }
    }

    @Nested
    @DisplayName("getUserPermissions() method")
    class GetUserPermissionsMethod {

        @Test
        @DisplayName("should return user permissions when user exists")
        void shouldReturnUserPermissionsWhenUserExists() {
            // Given
            UserPermissions permissions = UserPermissions.create(
                    USER_ID,
                    Set.of(Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ, Authority.EVENTS_READ)
            );

            when(permissionsRepository.findById(USER_ID)).thenReturn(Optional.of(permissions));

            // When
            UserPermissions result = service.getUserPermissions(USER_ID);

            // Then
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getDirectAuthorities())
                    .containsExactlyInAnyOrder(Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ, Authority.EVENTS_READ);
        }

        @Test
        @DisplayName("should return empty permissions when user has no permissions record")
        void shouldReturnEmptyPermissionsWhenUserHasNoPermissionsRecord() {
            // Given
            when(permissionsRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // When
            UserPermissions result = service.getUserPermissions(USER_ID);

            // Then
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getDirectAuthorities()).isEmpty();
        }
    }
}
