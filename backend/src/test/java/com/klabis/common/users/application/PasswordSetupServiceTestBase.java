package com.klabis.common.users.application;

import com.klabis.common.email.EmailService;
import com.klabis.common.ratelimit.PerKeyRateLimiter;
import com.klabis.common.templating.ThymeleafTemplateRenderer;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.common.users.infrastructure.PasswordSetupTokenRepository;
import com.klabis.common.users.passwordsetup.TestConfigurationHelper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public abstract class PasswordSetupServiceTestBase {

    @Mock
    protected PasswordSetupTokenRepository tokenRepository;

    @Mock
    protected UserRepository userRepository;

    @Mock
    protected EmailService emailService;

    @Mock
    protected ThymeleafTemplateRenderer templateRenderer;

    @Mock
    protected PasswordEncoder passwordEncoder;

    @Mock
    protected PasswordComplexityValidator passwordValidator;

    @Mock
    protected PerKeyRateLimiter rateLimiter;

    protected PasswordSetupService createService() {
        return new PasswordSetupServiceImpl(
                tokenRepository,
                userRepository,
                emailService,
                templateRenderer,
                passwordEncoder,
                passwordValidator,
                rateLimiter,
                TestConfigurationHelper.createDefaultPasswordSetupProperties(),
                TestConfigurationHelper.createDefaultClubProperties()
        );
    }
}
