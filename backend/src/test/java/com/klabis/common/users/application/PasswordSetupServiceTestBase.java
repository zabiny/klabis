package com.klabis.common.users.application;

import com.klabis.common.email.EmailService;
import com.klabis.common.ratelimit.PerKeyRateLimiter;
import com.klabis.common.templating.ThymeleafTemplateRenderer;
import com.klabis.common.users.domain.UserRepository;
import com.klabis.common.users.domain.PasswordSetupTokenRepository;
import com.klabis.common.users.passwordsetup.TestConfigurationHelper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
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

    @Mock
    protected ActivationContactVerifier activationContactVerifier;

    protected PasswordSetupService createService() {
        return createService(fixedProvider(activationContactVerifier));
    }

    protected PasswordSetupService createService(ObjectProvider<ActivationContactVerifier> activationContactVerifierProvider) {
        return new PasswordSetupServiceImpl(
                tokenRepository,
                userRepository,
                emailService,
                templateRenderer,
                passwordEncoder,
                passwordValidator,
                rateLimiter,
                activationContactVerifierProvider,
                TestConfigurationHelper.createDefaultPasswordSetupProperties(),
                TestConfigurationHelper.createDefaultClubProperties()
        );
    }

    /**
     * A minimal {@link ObjectProvider} always resolving to the given value (or absent, for
     * {@code null}) - {@code ObjectProvider} cannot be a lambda target since it also extends
     * {@code Iterable}.
     */
    protected static <T> ObjectProvider<T> fixedProvider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return value;
            }

            @Override
            public java.util.Iterator<T> iterator() {
                return (value == null ? java.util.List.<T>of() : java.util.List.of(value)).iterator();
            }
        };
    }
}
