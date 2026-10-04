package com.klabis.common;

import com.klabis.common.authorization.AuthorizationEvaluator;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Setup for infrastructure tests of {@code common} (filters, advices, HAL support) that declare their own inner
 * test controller via {@code @WebMvcTest(controllers = ...)}. Each such test deliberately keeps its own context;
 * this annotation only restricts it to {@code common} beans, so other modules' web beans and their mocks are not needed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@ModuleSlicing(module = "common", mode = ApplicationModuleTest.BootstrapMode.STANDALONE, verifyAutomatically = false)
@ActiveProfiles("test")
@Import({ClockConfiguration.class, AuthorizationEvaluator.class, FixedAuthorizationSnapshotConfiguration.class})
@CommonWebMvcMockitoBeans
public @interface CommonInfrastructureWebMvcSetup {
}
