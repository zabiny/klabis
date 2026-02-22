package com.klabis.users.application;

import com.klabis.users.*;
import com.klabis.users.persistence.UserPermissionsRepository;
import com.klabis.users.persistence.UserRepository;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@Transactional
class UserServiceImpl implements UserService {

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
    public UserId createUser(String username, String email, Set<Authority> authorities) {
        log.debug("Creating user pending password setup: username={}, email={}", username, email);

        User user = User.createdUserWithEmail(username, email);

        User savedUser = userRepository.save(user);
        UserId userId = savedUser.getId();

        UserPermissions permissions = UserPermissions.create(userId, authorities);
        userPermissionsRepository.save(permissions);

        log.info("Created user pending password setup: userId={}, username={}", userId, username);
        return userId;
    }

    @Override
    public UserId createActiveUser(String username, String passwordHash, Set<Authority> authorities) {
        log.debug("Creating active user: username={}", username);

        User user = User.createdUser(username, passwordHash);

        User savedUser = userRepository.save(user);
        UserId userId = savedUser.getId();

        UserPermissions permissions = UserPermissions.create(userId, authorities);
        userPermissionsRepository.save(permissions);

        log.info("Created active user: userId={}", userId);
        return userId;
    }

    @Override
    public Optional<User> findUserByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            log.debug("User found: userId={}", user.get().getId());
        } else {
            log.debug("User not found for username: {}", username);
        }
        return user;
    }
}
