package com.klabis.users.passwordsetup;

import com.klabis.common.PIDataMaskingUtil;
import com.klabis.common.email.EmailMessage;
import com.klabis.common.email.EmailService;
import com.klabis.common.email.EmailTemplate;
import com.klabis.common.ratelimit.PerKeyRateLimiter;
import com.klabis.common.ratelimit.RateLimitExceededException;
import com.klabis.common.templating.ThymeleafTemplateRenderer;
import com.klabis.users.AccountStatus;
import com.klabis.users.PasswordSetupToken;
import com.klabis.users.TokenHash;
import com.klabis.users.User;
import com.klabis.users.persistence.PasswordSetupTokenRepository;
import com.klabis.users.persistence.UserRepository;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

/**
 * Application service for password setup flow.
 *
 * <p>This service handles the complete password setup process for new users:
 * <ul>
 *   <li>Generating tokens and sending setup emails</li>
 *   <li>Validating tokens before showing password form</li>
 *   <li>Completing password setup and activating accounts</li>
 *   <li>Reissuing tokens for expired tokens with rate limiting</li>
 * </ul>
 *
 * <p><b>Event-Driven Architecture:</b> This service works with Spring Modulith events:
 * <ul>
 *   <li>Listens for MemberCreatedEvent to send initial password setup emails</li>
 *   <li>Token reissuance requires member data to be passed in (via Member module query)</li>
 * </ul>
 */
@Service
@PrimaryPort
public class PasswordSetupService {

    private static final Logger log = LoggerFactory.getLogger(PasswordSetupService.class);

    private final PasswordSetupTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ThymeleafTemplateRenderer templateRenderer;
    private final PasswordEncoder passwordEncoder;
    private final PasswordComplexityValidator passwordValidator;
    private final PerKeyRateLimiter rateLimiter;

    @Value("${password-setup.token.expiration-hours:4}")
    private int tokenExpirationHours;

    @Value("${password-setup.email.base-url:https://localhost:8443}")
    private String baseUrl;

    @Value("${klabis.club.name:Klabis}")
    private String clubName;

    public PasswordSetupService(
            PasswordSetupTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            ThymeleafTemplateRenderer templateRenderer,
            PasswordEncoder passwordEncoder,
            PasswordComplexityValidator passwordValidator,
            PerKeyRateLimiter rateLimiter) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.templateRenderer = templateRenderer;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Generates a password setup token for a user and sends them an email.
     *
     * <p>This is called during member registration to initiate the password setup flow.
     *
     * @param user the user requiring password setup
     * @return the generated token (for immediate email sending)
     * @throws IllegalArgumentException if user is null or already active
     */
    @Transactional
    public GeneratedTokenResult generateToken(User user) {
        Assert.notNull(user, "User is required");
        Assert.state(user.getAccountStatus() == AccountStatus.PENDING_ACTIVATION,
                "User must be in PENDING_ACTIVATION status");

        // Invalidate any existing tokens for this user
        tokenRepository.invalidateAllForUser(user.getId());

        // Generate new token
        Duration validity = Duration.ofHours(tokenExpirationHours);
        PasswordSetupToken token = PasswordSetupToken.generateFor(user, validity);
        tokenRepository.save(token);

        return new GeneratedTokenResult(token, token.getPlainText());
    }

    /**
     * Sends a password setup email using member data.
     *
     * <p>This method is called from event handlers with member data passed in.
     * Uses the member's first name for a personalized greeting.
     *
     * @param firstName  the member's first name
     * @param email      the email address to send to
     * @param plainToken the plain text token (not hashed)
     */
    @Transactional
    public void sendPasswordSetupEmail(String firstName, String email, String plainToken) {
        Assert.hasText(firstName, "First name is required");
        Assert.hasText(email, "Email is required");
        Assert.hasText(plainToken, "Token is required");

        String setupUrl = buildSetupUrl(plainToken);
        String maskedEmail = PIDataMaskingUtil.maskEmail(email);

        Map<String, Object> templateVariables = Map.of(
                "firstName", firstName,
                "setupUrl", setupUrl,
                "expirationHours", tokenExpirationHours,
                "clubName", clubName
        );

        String htmlBody = templateRenderer.renderHtml(EmailTemplate.PASSWORD_SETUP, templateVariables);
        String textBody = templateRenderer.renderText(EmailTemplate.PASSWORD_SETUP, templateVariables);

        EmailMessage message = EmailMessage.multipart(
                email,
                "Set Your Password for " + clubName,
                htmlBody,
                textBody
        );

        emailService.send(message);
        log.info("Sent password setup email to {}", maskedEmail);
    }

    /**
     * Sends a password setup email using username for greeting (no member data).
     *
     * <p>This method is called from UserCreatedEventHandler when only user data is available
     * (no member personal data). Uses the username (registration number) for the greeting.
     *
     * @param username   the user's username (registration number)
     * @param email      the email address to send to
     * @param plainToken the plain text token (not hashed)
     */
    @Transactional
    public void sendPasswordSetupEmailWithUsername(String username, String email, String plainToken) {
        Assert.hasText(username, "Username is required");
        Assert.hasText(email, "Email is required");
        Assert.hasText(plainToken, "Token is required");

        String setupUrl = buildSetupUrl(plainToken);
        String maskedEmail = PIDataMaskingUtil.maskEmail(email);

        Map<String, Object> templateVariables = Map.of(
                "firstName", username,  // Use username as greeting name
                "setupUrl", setupUrl,
                "expirationHours", tokenExpirationHours,
                "clubName", clubName
        );

        String htmlBody = templateRenderer.renderHtml(EmailTemplate.PASSWORD_SETUP, templateVariables);
        String textBody = templateRenderer.renderText(EmailTemplate.PASSWORD_SETUP, templateVariables);

        EmailMessage message = EmailMessage.multipart(
                email,
                "Set Your Password for " + clubName,
                htmlBody,
                textBody
        );

        emailService.send(message);
        log.info("Sent password setup email to {}", maskedEmail);
    }

    /**
     * Validates a password setup token.
     *
     * <p>This is called when the user opens the password setup page to verify the token is valid.
     *
     * @param plainToken the plain text token from the email link
     * @return validation result with expiration time (no email for security)
     * @throws TokenValidationException if token is invalid, expired, or already used
     */
    @Transactional(readOnly = true)
    public ValidateTokenResponse validateToken(String plainToken) {
        PasswordSetupToken token = validateTokenAndGetUser(plainToken);

        // Get user and verify account status
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new TokenValidationException("User not found"));

        if (user.getAccountStatus() != AccountStatus.PENDING_ACTIVATION) {
            throw new TokenValidationException("Account is not in pending activation status");
        }

        log.info("Validated password setup token for user {}", user.getId());

        // Return validation without email for security reasons
        return new ValidateTokenResponse(true, token.getExpiresAt());
    }

    /**
     * Completes the password setup flow by setting the user's password and activating the account.
     *
     * @param request   the password setup request
     * @param ipAddress the IP address of the user
     * @throws TokenValidationException    if token is invalid
     * @throws PasswordValidationException if password fails complexity validation
     */
    @Transactional
    public PasswordSetupResponse completePasswordSetup(PasswordSetupRequest request, String ipAddress) {
        // Validate token
        PasswordSetupToken token = validateTokenAndGetUser(request.token());

        // Get user
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new TokenValidationException("User not found"));

        // Validate password confirmation
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new PasswordValidationException("Passwords do not match");
        }

        // Validate password complexity (without member context for now)
        try {
            passwordValidator.validateBasic(request.password());
        } catch (PasswordValidationException e) {
            throw new PasswordValidationException(e.getMessage());
        }

        // Hash new password
        String newPasswordHash = passwordEncoder.encode(request.password());

        // Activate user with new password
        User activatedUser = user.activateWithPassword(newPasswordHash);
        userRepository.save(activatedUser);

        // Mark token as used
        token.markAsUsed(ipAddress);
        tokenRepository.save(token);

        log.info("Completed password setup for user {}", user.getId());

        return new PasswordSetupResponse(
                "Password set successfully. You can now log in.",
                user.getUsername().toString()
        );
    }

    /**
     * Requests a new password setup token for a user.
     *
     * <p>This method allows users to request a new token if their previous token expired.
     * It invalidates any existing tokens for the user and generates a new one.
     *
     * <p>Rate limited to 3 requests per hour per registration number to prevent abuse.
     *
     * <p><b>Note:</b> This method should be called from the members module, which provides
     * the member's email for sending the password setup email.
     *
     * @param registrationNumber the registration number of the user (format: XXXYYDD)
     * @param email              the member's email address (required for sending password setup email)
     * @throws TokenValidationException   if user not found, or account is not pending activation
     * @throws IllegalArgumentException   if registration number format is invalid or email is missing
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    @Transactional
    public void requestNewToken(String registrationNumber, String email) {
        Assert.hasText(email, "Email is required");

        // 0. Check rate limit for this registration number (before any other processing)
        rateLimiter.checkLimit(registrationNumber);

        // 1. Find user by registration number
        User user = userRepository.findByUsername(registrationNumber)
                .orElseThrow(() -> new TokenValidationException("User not found"));

        // 2. Check account status - only allow token reissuance for pending activation
        if (user.getAccountStatus() != AccountStatus.PENDING_ACTIVATION) {
            throw new TokenValidationException(
                    "Account is not in pending activation status. Token reissuance is only available for accounts awaiting activation."
            );
        }

        // 3. Generate new token (this automatically invalidates old tokens)
        GeneratedTokenResult result = generateToken(user);

        // 4. Send password setup email
        sendPasswordSetupEmail("User", email, result.plainToken());

        log.info("Password setup token reissued for user {}", user.getId());
    }

    /**
     * Builds the password setup URL using UriComponentsBuilder.
     *
     * @param plainToken the plain text token
     * @return the full setup URL
     */
    private String buildSetupUrl(String plainToken) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/password-setup")
                .queryParam("token", plainToken)
                .build()
                .toUriString();
    }

    /**
     * Validates a token and retrieves the associated user.
     *
     * <p>This method extracts the common token validation logic used by both
     * {@link #validateToken(String)} and {@link #completePasswordSetup(PasswordSetupRequest, String)}.
     *
     * @param plainToken the plain text token to validate
     * @return a TokenValidationResult containing the token and user
     * @throws TokenExpiredException     if the token has expired
     * @throws TokenAlreadyUsedException if the token has already been used
     * @throws TokenValidationException  if the token or user is invalid
     */
    private PasswordSetupToken validateTokenAndGetUser(String plainToken) {
        TokenHash tokenHash = TokenHash.hash(plainToken);

        PasswordSetupToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenValidationException("Invalid token"));

        // Check if token is expired
        if (token.isExpired()) {
            throw new TokenExpiredException("Token has expired");
        }

        // Check if token has been used
        if (token.isUsed()) {
            throw new TokenAlreadyUsedException("Token has already been used");
        }

        return token;
    }
}
