package com.klabis.common.users;

/**
 * A login is unique across the usernames and altUsernames of all users.
 * <p>
 * Deliberately not a user-facing error: callers that take the login from user input translate it
 * (registration reports it as a registration number already in use).
 */
public class UsernameAlreadyInUseException extends RuntimeException {

    public UsernameAlreadyInUseException(String username) {
        super("Username '" + username + "' is already in use");
    }
}
