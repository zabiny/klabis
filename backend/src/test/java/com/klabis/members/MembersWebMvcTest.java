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
 * No {@code controllers} filter: {@link ModuleSlicing} in STANDALONE mode scans only the members module
 * (plus shared {@code common}), so every members controller and RepresentationModelProcessor is real.
 * {@code groups} and {@code calendar} are included because their processors add links to member responses
 * (training group, ical-token); other modules are not loaded. Ports excluded by the web slice are mocked via
 * the per-module {@code *WebMvcMockitoBeans} annotations. Tests stub them through {@code @Autowired} fields;
 * declaring an additional {@code @MockitoBean} in a test class would create a different context.
 * <p>
 * {@link SynchronizationPort} is mocked directly: {@code MemberController} uses it for the sync link, while
 * the sync module itself is not loaded.
 * <p>
 * {@link MemberDiscoveryPort} is mocked here because it is an {@code Optional} feature flag and so is
 * excluded from {@link MembersWebMvcMockitoBeans}.
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
