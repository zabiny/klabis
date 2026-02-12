package com.klabis.users.passwordsetup;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for password setup flow.
 *
 * <p>Provides public endpoints for new users to set their passwords:
 * <ul>
 *   <li>Validate token before showing password form</li>
 *   <li>Complete password setup and activate account</li>
 *   <li>Request new token if previous one expired</li>
 * </ul>
 */
@RestController
@Tag(name = "Password Setup", description = "Password setup and account activation API")
@PrimaryAdapter
public class PasswordSetupController {

    private static final Logger log = LoggerFactory.getLogger(PasswordSetupController.class);

    private final PasswordSetupService passwordSetupService;

    public PasswordSetupController(PasswordSetupService passwordSetupService) {
        this.passwordSetupService = passwordSetupService;
    }

    /**
     * Validates a password setup token.
     *
     * <p>This endpoint is called when the user opens the password setup page
     * to verify that the token is valid before showing the password form.
     *
     * @param token the plain text token from the email link
     * @return validation response with masked email and expiration time
     */
    @GetMapping("/api/auth/password-setup/validate")
    @Operation(summary = "Validate password setup token", description = "Validates a token before showing the password setup form")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid", content = @Content(schema = @Schema(implementation = ValidateTokenResponse.class))),
    })
    @Parameter(name = "token", description = "The plain text token from the email link", required = true, example = "abc123def456")
    public ResponseEntity<ValidateTokenResponse> validateToken(@RequestParam @NotBlank String token) {
        ValidateTokenResponse response = passwordSetupService.validateToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Completes the password setup flow.
     *
     * <p>This endpoint is called when the user submits the password setup form
     * with their chosen password.
     *
     * @param request     the password setup request
     * @param httpRequest the HTTP request (for IP address extraction)
     * @return success response with registration number
     */
    @PostMapping("/api/auth/password-setup/complete")
    @Operation(summary = "Complete password setup", description = "Sets the user's password and activates the account")
    @ApiResponse(responseCode = "200", description = "Password set successfully")
    public ResponseEntity<PasswordSetupResponse> completePasswordSetup(
            @Valid @RequestBody SetPasswordRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);

        PasswordSetupRequest serviceRequest =
                new PasswordSetupRequest(
                        request.token(),
                        request.password(),
                        request.passwordConfirmation()
                );

        PasswordSetupResponse response =
                passwordSetupService.completePasswordSetup(serviceRequest, ipAddress);

        return ResponseEntity.ok(response);
    }

    /**
     * Requests a new password setup token.
     *
     * <p>This endpoint is used when the previous token has expired.
     * A new token will be sent to the member's email address if the account
     * is in PENDING_ACTIVATION status.
     *
     * <p>Rate limited to 3 requests per hour per registration number.
     *
     * <p><b>Note:</b> This endpoint requires member data and should be called
     * from the members module. The members module will provide the email address.
     *
     * @param request the token request containing registration number and email
     * @return success message (generic response for security)
     */
    @PostMapping("/api/auth/password-setup/request")
    @Operation(summary = "Request new password setup token", description = "Requests a new token if the previous one expired")
    @ApiResponse(responseCode = "200", description = "Request processed successfully")
    public ResponseEntity<TokenRequestResponse> requestNewToken(@Valid @RequestBody TokenRequestRequest request) {
        passwordSetupService.requestNewToken(request.registrationNumber(), request.email());
        return ResponseEntity.ok(new TokenRequestResponse(
                "If your account is pending activation, you will receive an email with a new setup link."));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Handle multiple IPs in X-Forwarded-For (take first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        // Fallback to unknown if all sources are empty
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = "unknown";
        }
        return ip;
    }

    @ApiResponse(
            responseCode = "410",
            description = "Token expired or already used",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
    )
    @ExceptionHandler(TokenExpiredException.class)
    public ErrorResponse handleTokenExpired(TokenExpiredException e) {
        return ErrorResponse.create(e, HttpStatus.GONE, "Token has expired. Please request a new one.");
    }

    @ApiResponse(
            responseCode = "410",
            description = "Token expired or already used",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
    )
    @ExceptionHandler(TokenAlreadyUsedException.class)
    public ErrorResponse handleTokenAlreadyUsed(TokenAlreadyUsedException e) {
        return ErrorResponse.create(e, HttpStatus.GONE, "Token has already been used. Please request a new one.");
    }
}
