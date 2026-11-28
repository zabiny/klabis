package com.klabis;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.modulith.test.ApplicationModuleTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for cross-module integration tests
 * <p>
 * Starts up tested module and all dependent modules. Then test should perform tested flow by emulating API calls (using MockMvc) and verify expected responses from API. Flow typically consists from multiple requests
 */
@KlabisModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES)
@AutoConfigureMockMvc
// Because events are synchronized to transactions, triggering transaction must succeed and hence data will be saved into DB. We must cleanup these data manually
@CleanupTestData
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(TestApplicationConfiguration.class)
public @interface CrossModuleTest {

    @AliasFor(annotation = KlabisModuleTest.class, attribute = "extraIncludes")
    String[] extraIncludes() default {};
}
