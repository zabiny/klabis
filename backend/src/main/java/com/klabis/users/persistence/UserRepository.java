package com.klabis.users.persistence;

import com.klabis.users.User;
import com.klabis.users.UserId;
import com.klabis.users.Users;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Internal repository interface for User aggregate.
 * <p>
 * Defines methods to persist and retrieve User entities.
 * <p>
 * Extends {@link Users} public API with write operations (save, delete).
 *
 * @apiNote This is internal API for use within the users module only.
 * Other modules should use {@link com.klabis.users.Users Users} interface
 * for read operations and {@link com.klabis.users.UserService UserService}
 * for command operations.
 */
@SecondaryPort
public interface UserRepository extends Users {

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
