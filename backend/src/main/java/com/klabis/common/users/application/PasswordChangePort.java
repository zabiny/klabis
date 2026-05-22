package com.klabis.common.users.application;

import com.klabis.common.users.UserId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * Application port for changing the password of an authenticated user.
 */
@PrimaryPort
public interface PasswordChangePort {

    record ChangePasswordCommand(UserId userId, String currentPassword, String newPassword) {
    }

    /**
     * Changes the password of the given user after verifying the current password.
     *
     * @param command the change password command
     * @throws IncorrectCurrentPasswordException if currentPassword does not match the stored hash
     * @throws com.klabis.common.users.domain.PasswordValidationException if newPassword fails complexity rules
     */
    void changePassword(ChangePasswordCommand command);
}
