package com.klabis.common.users.domain;

import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

/**
 * Internal repository interface for User aggregate.
 * <p>
 * Defines methods to persist and retrieve User entities.
 * <p>
 *
 * @apiNote This is internal API for use within the users module only.
 * for read operations and {@link UserService UserService}
 * for command operations.
 */
@SecondaryPort
public interface UserRepository {

    /**
     * Find a user by their unique ID.
     *
     * @param id the user's ID
     * @return optional containing the user if found
     */
    Optional<User> findById(UserId id);

    /**
     * Find a user by their username.
     *
     * @param username the username
     * @return optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    /**
     * Save a user.
     *
     * @param user the user to save
     * @return the saved user
     */
    User save(User user);

    /**
     * Delete user by ID.
     * <p>
     * This is an internal method not exposed in the public Users API.
     *
     * @param id the user ID to delete
     */
    void deleteById(UserId id);

    /**
     * Check if user exists by ID.
     * <p>
     * This is an internal method not exposed in the public Users API.
     *
     * @param id the user ID to check
     * @return true if user exists, false otherwise
     */
    boolean existsById(UserId id);

    // Read methods inherited from Users public API:
    // - Optional<User> findById(UserId id)
    // - Optional<User> findByUsername(String username)
}
