package com.klabis.common;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.AuthorizationSnapshotProvider;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Beans every {@code @WebMvcTest} slice of the application needs on top of the web layer. The authorization
 * beans need no database: a user installed by {@link WithKlabisMockUser} carries its snapshot in the token.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({ClockConfiguration.class, EncryptionConfiguration.class, HalFormsSupport.class,
        AuthorizationEvaluator.class, AuthorizationSnapshotProvider.class})
public class KlabisWebMvcSliceConfiguration {
}
