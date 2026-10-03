package com.klabis.oris;

import com.klabis.common.ClockConfiguration;
import com.klabis.common.CommonWebMvcMockitoBeans;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Single shared {@code @WebMvcTest} context for all REST adapter tests of the oris module.
 * <p>
 * STANDALONE slicing loads oris and shared {@code common}. The ORIS feature (profile {@code oris}) is not part of
 * this context: {@code OrisController} is profile-gated and injects {@code OrisApiClient} directly, so the test
 * that needs it adds the profile and declares the client locally in a dedicated class.
 * <p>
 * Tests must stub mocks via {@code @Autowired} fields; a {@code @MockitoBean} declared in a test class
 * creates a new context and defeats the sharing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@WebMvcTest
@ModuleSlicing(module = "oris", mode = ApplicationModuleTest.BootstrapMode.STANDALONE, verifyAutomatically = false)
@ActiveProfiles("test")
@Import({ClockConfiguration.class, EncryptionConfiguration.class, HalFormsSupport.class})
@OrisWebMvcMockitoBeans
@CommonWebMvcMockitoBeans
public @interface OrisWebMvcTest {
}
