package com.klabis.common.users.passwordsetup;

import com.klabis.common.users.User;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface PasswordSetupService {

    GeneratedTokenResult generateToken(User user);

    void sendPasswordSetupEmail(String firstName, String email, String plainToken);

    void sendPasswordSetupEmailWithUsername(String username, String email, String plainToken);

    ValidateTokenResponse validateToken(String plainToken);

    PasswordSetupResponse completePasswordSetup(PasswordSetupRequest request, String ipAddress);

    void requestNewToken(String registrationNumber, String email);
}
