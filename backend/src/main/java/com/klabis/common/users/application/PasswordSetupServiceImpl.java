package com.klabis.common.users.application;

import com.klabis.common.ClubProperties;
import com.klabis.common.PIDataMaskingUtil;
import com.klabis.common.email.EmailMessage;
import com.klabis.common.email.EmailService;
import com.klabis.common.email.EmailTemplate;
import com.klabis.common.ratelimit.PerKeyRateLimiter;
import com.klabis.common.templating.ThymeleafTemplateRenderer;
import com.klabis.common.users.domain.*;
import com.klabis.common.users.domain.PasswordSetupTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

@Service
class PasswordSetupServiceImpl implements PasswordSetupService {

    private static final Logger log = LoggerFactory.getLogger(PasswordSetupServiceImpl.class);

    private final PasswordSetupTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ThymeleafTemplateRenderer templateRenderer;
    private final PasswordEncoder passwordEncoder;
    private final PasswordComplexityValidator passwordValidator;
    private final PerKeyRateLimiter rateLimiter;
    private final PasswordSetupProperties passwordSetupProperties;
    private final ClubProperties clubProperties;

    PasswordSetupServiceImpl(
            PasswordSetupTokenRepository tokenRepository,
            UserRepository userRepository,
            EmailService emailService,
            ThymeleafTemplateRenderer templateRenderer,
            PasswordEncoder passwordEncoder,
            PasswordComplexityValidator passwordValidator,
            PerKeyRateLimiter rateLimiter,
            PasswordSetupProperties passwordSetupProperties,
            ClubProperties clubProperties) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.templateRenderer = templateRenderer;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.rateLimiter = rateLimiter;
        this.passwordSetupProperties = passwordSetupProperties;
        this.clubProperties = clubProperties;
    }

    @Override
    @Transactional
    public GeneratedTokenResult generateToken(User user) {
        Assert.notNull(user, "User is required");
        Assert.state(user.getAccountStatus() == AccountStatus.PENDING_ACTIVATION,
                "User must be in PENDING_ACTIVATION status");

        tokenRepository.invalidateAllForUser(user.getId());

        Duration validity = Duration.ofHours(passwordSetupProperties.getToken().getExpirationHours());
        PasswordSetupToken token = PasswordSetupToken.generateFor(user.getId(), validity);
        tokenRepository.save(token);

        return new GeneratedTokenResult(token, token.getPlainText());
    }

    @Override
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
                "expirationHours", passwordSetupProperties.getToken().getExpirationHours(),
                "clubName", clubProperties.getName()
        );

        String htmlBody = templateRenderer.renderHtml(EmailTemplate.PASSWORD_SETUP, templateVariables);
        String textBody = templateRenderer.renderText(EmailTemplate.PASSWORD_SETUP, templateVariables);

        EmailMessage message = EmailMessage.multipart(
                email,
                "Set Your Password for " + clubProperties.getName(),
                htmlBody,
                textBody
        );

        emailService.send(message);
        log.info("Sent password setup email to {}", maskedEmail);
    }

    @Override
    @Transactional
    public void sendPasswordSetupEmailWithUsername(String username, String email, String plainToken) {
        Assert.hasText(username, "Username is required");
        Assert.hasText(email, "Email is required");
        Assert.hasText(plainToken, "Token is required");

        String setupUrl = buildSetupUrl(plainToken);
        String maskedEmail = PIDataMaskingUtil.maskEmail(email);

        Map<String, Object> templateVariables = Map.of(
                "firstName", username,
                "setupUrl", setupUrl,
                "expirationHours", passwordSetupProperties.getToken().getExpirationHours(),
                "clubName", clubProperties.getName()
        );

        String htmlBody = templateRenderer.renderHtml(EmailTemplate.PASSWORD_SETUP, templateVariables);
        String textBody = templateRenderer.renderText(EmailTemplate.PASSWORD_SETUP, templateVariables);

        EmailMessage message = EmailMessage.multipart(
                email,
                "Set Your Password for " + clubProperties.getName(),
                htmlBody,
                textBody
        );

        emailService.send(message);
        log.info("Sent password setup email to {}", maskedEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public PasswordSetupToken validateToken(String plainToken) {
        PasswordSetupToken token = findValidToken(plainToken);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new TokenValidationException("User not found"));

        if (user.getAccountStatus() != AccountStatus.PENDING_ACTIVATION) {
            throw new TokenValidationException("Account is not in pending activation status");
        }

        log.info("Validated password setup token for user {}", user.getId());

        return token;
    }

    @Override
    @Transactional
    public User completePasswordSetup(SetupPasswordCommand command, String ipAddress) {
        PasswordSetupToken token = findValidToken(command.token());

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new TokenValidationException("User not found"));

        try {
            passwordValidator.validateBasic(command.password());
        } catch (PasswordValidationException e) {
            throw new PasswordValidationException(e.getMessage());
        }

        String newPasswordHash = passwordEncoder.encode(command.password());

        User activatedUser = user.activateWithPassword(newPasswordHash);
        User savedUser = userRepository.save(activatedUser);

        token.markAsUsed(ipAddress);
        tokenRepository.save(token);

        log.info("Completed password setup for user {}", user.getId());

        return savedUser;
    }

    @Override
    @Transactional
    public void requestNewToken(String registrationNumber, String email) {
        Assert.hasText(email, "Email is required");

        rateLimiter.checkLimit(registrationNumber);

        User user = userRepository.findByUsername(registrationNumber)
                .orElseThrow(() -> new TokenValidationException("User not found"));

        if (user.getAccountStatus() != AccountStatus.PENDING_ACTIVATION) {
            throw new TokenValidationException(
                    "Account is not in pending activation status. Token reissuance is only available for accounts awaiting activation."
            );
        }

        GeneratedTokenResult result = generateToken(user);

        sendPasswordSetupEmail("User", email, result.plainToken());

        log.info("Password setup token reissued for user {}", user.getId());
    }

    private String buildSetupUrl(String plainToken) {
        return UriComponentsBuilder.fromHttpUrl(passwordSetupProperties.getBaseUrl())
                .path("/password-setup")
                .queryParam("token", plainToken)
                .build()
                .toUriString();
    }

    private PasswordSetupToken findValidToken(String plainToken) {
        TokenHash tokenHash = TokenHash.hash(plainToken);

        PasswordSetupToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenValidationException("Invalid token"));

        if (token.isExpired()) {
            throw new TokenExpiredException("Token has expired");
        }

        if (token.isUsed()) {
            throw new TokenAlreadyUsedException("Token has already been used");
        }

        return token;
    }
}
