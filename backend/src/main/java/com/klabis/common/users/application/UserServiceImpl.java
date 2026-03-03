package com.klabis.common.users.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    @Override
    public void suspendUser(UserId userId) {
        log.debug("Suspending user: userId={}", userId);

        findById(userId).ifPresentOrElse(
                user -> {
                    if (user.getAccountStatus() != AccountStatus.SUSPENDED) {
                        User suspended = user.suspend();
                        userRepository.save(suspended);
                        log.info("Suspended user: userId={}", userId);
                    } else {
                        log.debug("User already suspended: userId={}", userId);
                    }
                },
                () -> log.debug("User not found for suspension: userId={}", userId)
        );
    }

    @Override
    public void resumeUser(UserId userId) {
        log.debug("Resuming user: userId={}", userId);

        findById(userId).ifPresentOrElse(
                user -> {
                    if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                        User resumed = user.resume();
                        userRepository.save(resumed);
                        log.info("Resumed user: userId={}", userId);
                    } else {
                        log.debug("User already active: userId={}", userId);
                    }
                },
                () -> log.debug("User not found for resume: userId={}", userId)
        );
    }

    private Optional<User> findById(UserId userId) {
        return userRepository.findById(userId);
    }
}
