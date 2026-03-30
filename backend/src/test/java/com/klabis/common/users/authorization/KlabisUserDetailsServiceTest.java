package com.klabis.common.users.authorization;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.AccountStatus;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserPermissions;
import com.klabis.common.users.domain.UserPermissionsRepository;
import com.klabis.common.users.infrastructure.restapi.KlabisUserDetailsService;
import com.klabis.common.users.testdata.UserTestDataBuilder;
import com.klabis.common.users.testdata.UserTestDataConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KlabisUserDetailsService Tests")
class KlabisUserDetailsServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserPermissionsRepository permissionsRepository;

    @InjectMocks
    private KlabisUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new KlabisUserDetailsService(userService, permissionsRepository);
    }

    @Test
    @DisplayName("should load user by registrationNumber")
    void shouldLoadUserByRegistrationNumber() {
        User user = UserTestDataBuilder.anAdminUser().build();
        when(userService.findUserByUsername(UserTestDataConstants.DEFAULT_ADMIN_USERNAME)).thenReturn(Optional.of(
                user));

        // Mock permissions repository
        UserPermissions permissions = UserPermissions.create(user.getId(), UserTestDataConstants.ADMIN_AUTHORITIES);
        when(permissionsRepository.findById(user.getId())).thenReturn(Optional.of(permissions));

        UserDetails userDetails = userDetailsService.loadUserByUsername(UserTestDataConstants.DEFAULT_ADMIN_USERNAME);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(UserTestDataConstants.DEFAULT_ADMIN_USERNAME);
        assertThat(userDetails.getPassword()).isEqualTo(UserTestDataConstants.DEFAULT_PASSWORD_HASH);
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException for unknown user")
    void shouldThrowExceptionForUnknownUser() {
        when(userService.findUserByUsername("ZBM9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ZBM9999"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("'admin' user should have all authorities")
    void shouldMapAdminRoleToAuthorities() {
        User user = UserTestDataBuilder.anAdminUser().build();
        when(userService.findUserByUsername(UserTestDataConstants.DEFAULT_ADMIN_USERNAME)).thenReturn(Optional.of(
                user));

        // Mock permissions repository
        UserPermissions permissions = UserPermissions.create(user.getId(), UserTestDataConstants.ADMIN_AUTHORITIES);
        when(permissionsRepository.findById(user.getId())).thenReturn(Optional.of(permissions));

        UserDetails userDetails = userDetailsService.loadUserByUsername(UserTestDataConstants.DEFAULT_ADMIN_USERNAME);

        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(Arrays.stream(Authority.values()).map(Authority::getValue).toArray(String[]::new));
    }

    @Test
    @DisplayName("should map ROLE_MEMBER to read-only authority")
    void shouldMapMemberRoleToAuthorities() {
        User user = UserTestDataBuilder.aMemberUser().build();
        when(userService.findUserByUsername(UserTestDataConstants.DEFAULT_MEMBER_USERNAME)).thenReturn(Optional.of(
                user));

        // Mock permissions repository
        UserPermissions permissions = UserPermissions.create(user.getId(), UserTestDataConstants.READ_ONLY_AUTHORITIES);
        when(permissionsRepository.findById(user.getId())).thenReturn(Optional.of(permissions));

        UserDetails userDetails = userDetailsService.loadUserByUsername(UserTestDataConstants.DEFAULT_MEMBER_USERNAME);

        assertThat(userDetails.getAuthorities())
                .extracting(auth -> auth.getAuthority())
                .containsExactly("MEMBERS:READ");
    }

    @Test
    @DisplayName("should not be enabled when account is SUSPENDED")
    void shouldNotBeEnabledWhenSuspended() {
        User user = UserTestDataBuilder.anAdminUser()
                .status(AccountStatus.SUSPENDED)
                .build();
        when(userService.findUserByUsername(UserTestDataConstants.DEFAULT_ADMIN_USERNAME)).thenReturn(Optional.of(
                user));

        // Mock permissions repository
        UserPermissions permissions = UserPermissions.create(user.getId(), UserTestDataConstants.ADMIN_AUTHORITIES);
        when(permissionsRepository.findById(user.getId())).thenReturn(Optional.of(permissions));

        UserDetails userDetails = userDetailsService.loadUserByUsername(UserTestDataConstants.DEFAULT_ADMIN_USERNAME);

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should not be enabled when account is PENDING_ACTIVATION")
    void shouldNotBeEnabledWhenPendingActivation() {
        User user = UserTestDataBuilder.aMemberUser()
                .status(AccountStatus.PENDING_ACTIVATION)
                .build();
        when(userService.findUserByUsername(UserTestDataConstants.DEFAULT_ADMIN_USERNAME)).thenReturn(Optional.of(
                user));

        // Mock permissions repository
        UserPermissions permissions = UserPermissions.create(user.getId(), UserTestDataConstants.READ_ONLY_AUTHORITIES);
        when(permissionsRepository.findById(user.getId())).thenReturn(Optional.of(permissions));

        UserDetails userDetails = userDetailsService.loadUserByUsername(UserTestDataConstants.DEFAULT_ADMIN_USERNAME);

        assertThat(userDetails.isEnabled()).isFalse();
    }
}
