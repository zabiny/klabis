package com.klabis.users.persistence;

import com.klabis.users.User;
import com.klabis.users.Authority;
import com.klabis.users.UserId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

/**
 * Internal repository interface for User aggregate.
 * <p>
 * Defines methods to persist and retrieve User entities.
 *
 * @apiNote This is internal API for use within the users module only.
 * Other modules should use {@link com.klabis.users.Users Users} interface
 * for read operations and {@link com.klabis.users.UserService UserService}
 * for command operations.
 */
@Repository
@SecondaryPort
public interface UserRepository {

    /**
     * Save a user.
     *
     * @param user the user to save
     * @return the saved user
     */
    User save(User user);

    /**
     * Find user by ID.
     *
     * @param id user ID
     * @return optional containing user if found
     */
    Optional<User> findById(UserId id);

    /**
     * Find user by username.
     *
     * @param username the username
     * @return optional containing user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Count active users with a specific authority.
     *
     * @param authority the authority to check for
     * @return count of active users with the specified authority
     */
    long countActiveUsersWithAuthority(Authority authority);

    /**
     * Delete user by ID.
     *
     * @param id the user ID to delete
     */
    void deleteById(UserId id);

    /**
     * Check if user exists by ID.
     *
     * @param id the user ID to check
     * @return true if user exists, false otherwise
     */
    boolean existsById(UserId id);
}
