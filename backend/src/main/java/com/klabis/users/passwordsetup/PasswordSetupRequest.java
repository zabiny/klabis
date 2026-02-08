package com.klabis.users.passwordsetup;

/**
 * Request DTO for password setup.
 *
 * @param token                the plain text token
 * @param password             the new password
 * @param passwordConfirmation password confirmation
 */
public record PasswordSetupRequest(String token, String password, String passwordConfirmation) {
}
