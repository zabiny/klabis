package com.klabis.common.users.passwordsetup;

import com.klabis.common.users.PasswordSetupToken;

/**
 * Result object containing both the token entity and plain text token.
 *
 * @param token      the token entity
 * @param plainToken the plain text token (for email sending only)
 */
public record GeneratedTokenResult(PasswordSetupToken token, String plainToken) {
}
