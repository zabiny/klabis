package com.klabis.events;

import com.klabis.common.ClockConfiguration;
import com.klabis.common.CommonWebMvcMockitoBeans;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.events.infrastructure.restapi.EventsWebMvcMockitoBeans;
import com.klabis.finance.infrastructure.restapi.FinanceWebMvcMockitoBeans;
import com.klabis.members.infrastructure.restapi.MembersWebMvcMockitoBeans;
import com.klabis.sync.infrastructure.restapi.SyncWebMvcMockitoBeans;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Single shared {@code @WebMvcTest} context for all REST adapter tests of the events module.
 * <p>
 * STANDALONE slicing loads events and shared {@code common}; {@code members} is included because the
 * {@code Members} lookup and the {@code @ActingMember} argument resolver live there, {@code sync} because
 * the event and discipline controllers inject {@code SynchronizationPort}, and {@code finance} because
 * {@code RegistrationRecordTransactionLinkProcessor} uses the real {@code FinanceAccountLinkSupport}.
 * Every collaborator is a primary port mocked through the per-module {@code *WebMvcMockitoBeans}.
 * <p>
 * The ORIS feature (profile {@code oris}) is not part of this context: {@code OrisEventImportPort} is
 * {@code Optional} and tests that need it declare it locally in a dedicated class.
 * <p>
 * Tests must stub mocks via {@code @Autowired} fields; a {@code @MockitoBean} declared in a test class
 * creates a new context and defeats the sharing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@WebMvcTest
@ModuleSlicing(module = "events", extraIncludes = {"members", "sync", "finance"}, mode = ApplicationModuleTest.BootstrapMode.STANDALONE, verifyAutomatically = false)
@ActiveProfiles("test")
@Import({ClockConfiguration.class, EncryptionConfiguration.class, HalFormsSupport.class})
@EventsWebMvcMockitoBeans
@CommonWebMvcMockitoBeans
@MembersWebMvcMockitoBeans
@SyncWebMvcMockitoBeans
@FinanceWebMvcMockitoBeans
public @interface EventsWebMvcTest {
}
