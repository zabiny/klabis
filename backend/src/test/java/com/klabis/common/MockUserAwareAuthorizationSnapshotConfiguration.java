package com.klabis.common;

import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.authorization.AuthorizationSnapshotProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * For full application contexts: users installed by {@link WithKlabisMockUser} get the snapshot attached to
 * their authentication (they have no row in the database), every other authentication - a real access token -
 * goes through the database-backed request-scoped provider.
 * <p>
 * Wraps the production provider instead of adding a second one, because the production provider is already the
 * primary candidate.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MockUserAwareAuthorizationSnapshotConfiguration {

    private static final String REQUEST_SCOPED_PROVIDER = "requestScopedAuthorizationSnapshotProvider";

    @Bean
    static BeanPostProcessor mockUserAwareSnapshotProviderPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (REQUEST_SCOPED_PROVIDER.equals(beanName) && bean instanceof AuthorizationSnapshotProvider real) {
                    return (AuthorizationSnapshotProvider) () -> mockUserSnapshot().orElseGet(real::current);
                }
                return bean;
            }
        };
    }

    private static java.util.Optional<AuthorizationSnapshot> mockUserSnapshot() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getDetails() instanceof AuthorizationSnapshot fixed
                ? java.util.Optional.of(fixed)
                : java.util.Optional.empty();
    }
}
