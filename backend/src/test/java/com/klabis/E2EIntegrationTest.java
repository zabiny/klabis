package com.klabis;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for E2E tests
 * <p>
 * E2E test starts up tested module and all dependent modules. Then test should perform tested flow by emulating API calls (using MockMvc) and verify expected responses from API. Flow typically consists from multiple requests
 * <p>
 * NOTE: verifyAutomatically = false disables Spring Modulith's automatic
 * verification of module dependencies for E2E tests. Module dependency restrictions
 * should be verified in separate ModularityTests, not in E2E integration tests.
 */
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES, verifyAutomatically = false)
@AutoConfigureMockMvc
@ActiveProfiles("test")
// Because events are synchronized to transactions, triggering transaction must succeed and hence data will be saved into DB. We must cleanup these data manually
@CleanupTestData
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(TestApplicationConfiguration.class)
public @interface E2EIntegrationTest {

}
