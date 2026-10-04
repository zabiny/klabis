package com.klabis.members;

import com.klabis.calendar.CalendarWebMvcMockitoBeans;
import com.klabis.common.ClockConfiguration;
import com.klabis.common.CommonWebMvcMockitoBeans;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.FixedAuthorizationSnapshotConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.groups.GroupsWebMvcMockitoBeans;
import com.klabis.sync.SyncWebMvcMockitoBeans;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Single shared {@code @WebMvcTest} context for all REST adapter tests of the members module.
 * <p>
 * STANDALONE slicing loads only members and shared {@code common}; {@code groups}, {@code calendar} and {@code sync} are
 * added via {@code extraIncludes} because their processors add links to member responses. Every other
 * collaborator is a primary port mocked through the per-module {@code *WebMvcMockitoBeans}.
 * <p>
 * Optional / feature-flag beans (e.g. {@code MemberDiscoveryPort}) are deliberately mocked by none of the
 * group annotations; the tests that need them declare the mock themselves.
 * <p>
 * {@code UserDetailsService} stays mocked in {@link CommonWebMvcMockitoBeans}: it is security infrastructure.
 * <p>
 * Tests must stub mocks via {@code @Autowired} fields; a {@code @MockitoBean} declared in a test class
 * creates a new context and defeats the sharing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@WebMvcTest
@ModuleSlicing(module = "members", extraIncludes = {"groups", "calendar", "sync"}, mode = ApplicationModuleTest.BootstrapMode.STANDALONE, verifyAutomatically = false)
@ActiveProfiles("test")
@Import({ClockConfiguration.class, EncryptionConfiguration.class, HalFormsSupport.class,
        AuthorizationEvaluator.class, FixedAuthorizationSnapshotConfiguration.class})
@MembersWebMvcMockitoBeans
@CommonWebMvcMockitoBeans
@GroupsWebMvcMockitoBeans
@CalendarWebMvcMockitoBeans
@SyncWebMvcMockitoBeans
public @interface MembersWebMvcTest {
}
