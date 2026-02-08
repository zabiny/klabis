package com.klabis.users.application;

import com.klabis.users.*;
import com.klabis.users.persistence.UserPermissionsRepository;
import com.klabis.users.persistence.UserRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Implementation of {@link UserService}.
 * <p>
 * This service encapsulates user management operations, coordinating between
 * User and UserPermissions aggregates in a single transaction.
 * <p>
 * Design:
 * - Application service that coordinates use cases
 * - Transactional boundaries for multi-step operations
 * - Implementation detail hidden from other modules
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserPermissionsRepository userPermissionsRepository;

    /**
     * Constructor with dependency injection.
     *
     * @param userRepository            the user repository
     * @param userPermissionsRepository the user permissions repository
     */
    public UserServiceImpl(
            UserRepository userRepository,
            UserPermissionsRepository userPermissionsRepository) {
        this.userRepository = userRepository;
        this.userPermissionsRepository = userPermissionsRepository;
    }

    @Override
    public UserId createUserPendingActivation(
            String username,
            String passwordHash,
            Set<Authority> authorities) {

        // Delegate to new method without email
        UserCreationParams params = UserCreationParams.builder()
                .username(username)
                .passwordHash(passwordHash)
                .authorities(authorities)
                .build();  // email is null

        return createUserPendingActivation(params);
    }

    @Override
    public UserId createUserPendingActivation(UserCreationParams params) {
        // Create user with PENDING_ACTIVATION status
        User user;
        if (params.getEmail().isPresent()) {
            user = User.createPendingActivationWithEmail(
                    params.username(),
                    params.passwordHash(),
                    params.getEmail().get()
            );
        } else {
            user = User.createPendingActivation(params.username(), params.passwordHash());
        }

        User savedUser = userRepository.save(user);
        UserId userId = savedUser.getId();

        // Create permissions with granted authorities
        UserPermissions permissions = UserPermissions.create(userId, params.authorities());
        userPermissionsRepository.save(permissions);

        return userId;
    }

    @Override
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
