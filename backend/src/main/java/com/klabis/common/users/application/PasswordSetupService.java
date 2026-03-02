package com.klabis.common.users.application;

import com.klabis.common.users.domain.GeneratedTokenResult;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.infrastructure.restapi.PasswordSetupRequest;
import com.klabis.common.users.infrastructure.restapi.PasswordSetupResponse;
import com.klabis.common.users.infrastructure.restapi.ValidateTokenResponse;
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
