package com.klabis.calendar;

import com.klabis.common.ClockConfiguration;
import com.klabis.common.CommonWebMvcMockitoBeans;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.members.infrastructure.restapi.MembersWebMvcMockitoBeans;
import com.klabis.sync.infrastructure.restapi.SyncWebMvcMockitoBeans;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Single shared {@code @WebMvcTest} context for all REST adapter tests of the calendar module.
 * <p>
 * STANDALONE slicing loads calendar and shared {@code common}; {@code members} is included because its
 * {@code @ActingMember} argument resolver is used by the calendar controller, and {@code sync} because members web
 * beans inject {@code SynchronizationPort}. Every collaborator is a primary port mocked
 * through the per-module {@code *WebMvcMockitoBeans}.
 * <p>
 * Tests must stub mocks via {@code @Autowired} fields; a {@code @MockitoBean} declared in a test class
 * creates a new context and defeats the sharing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@WebMvcTest
@ModuleSlicing(module = "calendar", extraIncludes = {"members", "sync"}, mode = ApplicationModuleTest.BootstrapMode.STANDALONE, verifyAutomatically = false)
@ActiveProfiles("test")
@Import({ClockConfiguration.class, EncryptionConfiguration.class, HalFormsSupport.class})
@CalendarWebMvcMockitoBeans
@CommonWebMvcMockitoBeans
@MembersWebMvcMockitoBeans
@SyncWebMvcMockitoBeans
public @interface CalendarWebMvcTest {
}
