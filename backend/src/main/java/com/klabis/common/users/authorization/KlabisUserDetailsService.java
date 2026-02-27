package com.klabis.common.users.authorization;

import com.klabis.common.users.Authority;
import com.klabis.common.users.User;
import com.klabis.common.users.UserPermissions;
import com.klabis.common.users.UserService;
import com.klabis.common.users.persistence.UserPermissionsRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UserDetailsService implementation that loads users by registrationNumber.
 * <p>
 * Loads authentication data (credentials) from {@link User} entity
 * and authorization data (authorities) from {@link UserPermissions} aggregate.
 * This separation enables authentication and authorization to evolve independently.
 */
@Service
public class KlabisUserDetailsService implements UserDetailsService {

    private final UserService userService;
    // TODO: hide permissions repository call behind UserService interface
    private final UserPermissionsRepository permissionsRepository;

    public KlabisUserDetailsService(UserService userService,
                                    UserPermissionsRepository permissionsRepository) {
        this.userService = userService;
        this.permissionsRepository = permissionsRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return loadKlabisUserDetails(username)
                .map(KlabisUserDetails::getSpringSecurityUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username
                ));
    }

    public Optional<KlabisUserDetails> loadKlabisUserDetails(String username) {
        return userService.findUserByUsername(username)
                .map(user -> {
                    // Load UserPermissions - treat missing as empty authorities
                    UserPermissions permissions = permissionsRepository.findById(user.getId())
                            .orElse(UserPermissions.empty(user.getId()));

                    return new KlabisUserDetails(user, permissions);

                });
    }

    /**
     * Spring Security UserDetails adapter for User and UserPermissions.
     * <p>
     * Combines authentication data from {@link User} entity
     * with authorization data from {@link UserPermissions} aggregate.
     * <p>
     * Public visibility allows external code (e.g., JWT customizers) to access
     * the underlying User entity for type-safe operations like adding user_id claims.
     */
    public static class KlabisUserDetails implements UserDetails {

        private final User user;
        private final UserPermissions permissions;

        // Convert to `UserDetails` "compatible" with Authorizations JDBC storage.
        // (Using custom class which may return list of authoritieis as immutable class ends in deserialization error when retrieving authorization for refresh token and refresh token doesn't work then)
        public UserDetails getSpringSecurityUserDetails() {
            return org.springframework.security.core.userdetails.User.withUserDetails(this).build();
        }

        public KlabisUserDetails(User user, UserPermissions permissions) {
            this.user = user;
            this.permissions = permissions;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            // Get authorities from UserPermissions aggregate, not User entity
            Set<Authority> authoritySet = permissions.getDirectAuthorities();
            return authoritySet.stream()
                    .map(Authority::getValue)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        }

        @Override
        public String getPassword() {
            return user.getPasswordHash();
        }

        @Override
        public String getUsername() {
            return user.getUsername();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return user.isAuthenticatable();
        }

        public User getUser() {
            return user;
        }

        public UserPermissions getPermissions() {
            return permissions;
        }
    }
}
