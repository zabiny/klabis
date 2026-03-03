package com.klabis.common.users.application;

import com.klabis.common.users.domain.GeneratedTokenResult;
import com.klabis.common.users.domain.PasswordSetupToken;
import com.klabis.common.users.domain.User;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface PasswordSetupService {

    record SetupPasswordCommand(String token, String password) {
    }

    GeneratedTokenResult generateToken(User user);

    void sendPasswordSetupEmail(String firstName, String email, String plainToken);

    void sendPasswordSetupEmailWithUsername(String username, String email, String plainToken);

    PasswordSetupToken validateToken(String plainToken);

    User completePasswordSetup(SetupPasswordCommand command, String ipAddress);

    void requestNewToken(String registrationNumber, String email);
}
