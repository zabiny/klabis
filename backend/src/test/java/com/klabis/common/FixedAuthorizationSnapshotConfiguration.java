package com.klabis.common;

import com.klabis.common.authorization.AuthorizationSnapshotProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Import into slice tests that load {@code AuthorizationEvaluator} and must not touch the database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FixedAuthorizationSnapshotConfiguration {

    @Bean
    @Primary
    AuthorizationSnapshotProvider fixedAuthorizationSnapshotProvider() {
        return new FixedAuthorizationSnapshotProvider();
    }
}
