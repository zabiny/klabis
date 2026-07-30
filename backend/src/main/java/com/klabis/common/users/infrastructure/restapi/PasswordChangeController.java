package com.klabis.common.users.infrastructure.restapi;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.application.PasswordChangePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PrimaryAdapter
@RestController
@RequestMapping
@Tag(name = "MyProfile", description = "Current authenticated user operations")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
class PasswordChangeController implements MyProfileApi {

    private final PasswordChangePort passwordChangePort;

    PasswordChangeController(PasswordChangePort passwordChangePort) {
        this.passwordChangePort = passwordChangePort;
    }

    @Override
    @Operation(
            summary = "Change password",
            description = "Changes the current user's password. Requires the correct current password."
    )
    @ApiResponse(responseCode = "204", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Incorrect current password or new password fails complexity rules")
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
