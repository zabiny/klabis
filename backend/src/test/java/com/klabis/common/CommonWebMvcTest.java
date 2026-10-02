package com.klabis.common;

import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Single shared {@code @WebMvcTest} context for REST adapter tests of the {@code common} package
 * (root, dashboard, permission and password-setup controllers).
 * <p>
 * STANDALONE slicing loads only {@code common}: links that other modules contribute to the API root are
 * asserted in the tests of the contributing module. Infrastructure tests with their own inner test
 * controllers (filters, advices) deliberately keep dedicated contexts.
 * <p>
 * Tests must stub mocks via {@code @Autowired} fields; a {@code @MockitoBean} declared in a test class
 * creates a new context and defeats the sharing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@WebMvcTest
@ModuleSlicing(module = "common", mode = ApplicationModuleTest.BootstrapMode.STANDALONE, verifyAutomatically = false)
@ActiveProfiles("test")
@Import({ClockConfiguration.class, EncryptionConfiguration.class, HalFormsSupport.class})
@CommonWebMvcMockitoBeans
public @interface CommonWebMvcTest {
}
