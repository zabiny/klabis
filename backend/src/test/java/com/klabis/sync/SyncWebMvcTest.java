package com.klabis.sync;

import com.klabis.common.CommonWebMvcMockitoBeans;
import com.klabis.members.MembersWebMvcMockitoBeans;
import com.klabis.common.KlabisWebMvcSliceConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Single shared {@code @WebMvcTest} context for all REST adapter tests of the sync module.
 * <p>
 * STANDALONE slicing loads sync and shared {@code common}; {@code members} is included because its
 * {@code @ActingUser} argument resolver is used by the sync controller. Every collaborator is a primary port mocked
 * through the per-module {@code *WebMvcMockitoBeans}.
 * <p>
 * Tests must stub mocks via {@code @Autowired} fields; a {@code @MockitoBean} declared in a test class
 * creates a new context and defeats the sharing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@WebMvcTest
@ModuleSlicing(module = "sync", extraIncludes = {"members"}, mode = ApplicationModuleTest.BootstrapMode.STANDALONE, verifyAutomatically = false)
@ActiveProfiles("test")
@Import(KlabisWebMvcSliceConfiguration.class)
@SyncWebMvcMockitoBeans
@CommonWebMvcMockitoBeans
@MembersWebMvcMockitoBeans
public @interface SyncWebMvcTest {
}
