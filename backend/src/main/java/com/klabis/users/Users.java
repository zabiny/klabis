package com.klabis.users;

import org.jmolecules.architecture.hexagonal.Port;

import java.util.Optional;

/**
 * Public query API for User aggregate.
 * <p>
 * Provides read-only access to users for other modules.
 * This is the only public interface that should be used by external modules
 * to query user information.
 * <p>
 * For command operations (creating users, updating permissions), use {@link com.klabis.users.UserService UserService}.
 *
 * @see com.klabis.users.UserService
 */
@Port
public interface Users {

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
}
