package com.klabis.common.users.infrastructure.restapi;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.application.PasswordChangePort;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PrimaryAdapter
@RestController
@RequestMapping
class PasswordChangeController implements MyProfileApi {

    private final PasswordChangePort passwordChangePort;

    PasswordChangeController(PasswordChangePort passwordChangePort) {
        this.passwordChangePort = passwordChangePort;
    }

    @Override
    public ResponseEntity<Void> changePassword(
            ChangePasswordRequest request,
            Authentication authentication) {

        KlabisJwtAuthenticationToken token = (KlabisJwtAuthenticationToken) authentication;
        passwordChangePort.changePassword(
                new PasswordChangePort.ChangePasswordCommand(token.getUserId(), request.currentPassword(), request.newPassword())
        );

        return ResponseEntity.noContent().build();
    }
}
