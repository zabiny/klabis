package com.klabis.members;

import com.klabis.calendar.infrastructure.restapi.CalendarWebMvcMockitoBeans;
import com.klabis.common.ClockConfiguration;
import com.klabis.common.CommonWebMvcMockitoBeans;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.groups.GroupsWebMvcMockitoBeans;
import com.klabis.members.application.MemberDiscoveryPort;
import com.klabis.members.infrastructure.restapi.MembersWebMvcMockitoBeans;
import com.klabis.sync.application.SynchronizationPort;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.*;

/**
 * Single shared {@code @WebMvcTest} context for all REST adapter tests of the members module.
 * <p>
 * STANDALONE slicing loads only members and shared {@code common}; {@code groups} and {@code calendar} are
 * added via {@code extraIncludes} because their processors add links to member responses. Every other
 * collaborator is a primary port mocked through the per-module {@code *WebMvcMockitoBeans}.
 * <p>
 * Mocked here, not in those annotations:
 * <ul>
 *   <li>{@link MemberDiscoveryPort}: injected as {@code Optional}, so its presence is a feature flag.</li>
 *   <li>{@link SynchronizationPort}: sync web beans are not loaded, so {@code SyncWebMvcMockitoBeans}
 *       (which also mocks a sync-internal reader) is not composed; the port is needed only for the sync link.</li>
 * </ul>
 * {@code UserDetailsService} stays mocked in {@link CommonWebMvcMockitoBeans}: it is security infrastructure.
 * <p>
 * Tests must stub mocks via {@code @Autowired} fields; a {@code @MockitoBean} declared in a test class
 * creates a new context and defeats the sharing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@WebMvcTest
@ModuleSlicing(module = "members", extraIncludes = {"groups", "calendar"}, mode = ApplicationModuleTest.BootstrapMode.STANDALONE, verifyAutomatically = false)
@ActiveProfiles("test")
@Import({ClockConfiguration.class, EncryptionConfiguration.class, HalFormsSupport.class})
@MembersWebMvcMockitoBeans
@CommonWebMvcMockitoBeans
@GroupsWebMvcMockitoBeans
@CalendarWebMvcMockitoBeans
@MockitoBean(types = {MemberDiscoveryPort.class, SynchronizationPort.class})
public @interface MembersWebMvcTest {
}
