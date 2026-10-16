package com.klabis.notifications;

import com.klabis.common.CommonWebMvcMockitoBeans;
import com.klabis.common.KlabisWebMvcSliceConfiguration;
import com.klabis.members.MembersWebMvcMockitoBeans;
import com.klabis.sync.SyncWebMvcMockitoBeans;
import com.klabis.notifications.application.VapidProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Single shared {@code @WebMvcTest} context for REST adapter tests of the notifications module.
 * <p>
 * {@code members} is included because its {@code @ActingUser} argument resolver is used by the controller, and
 * {@code sync} because members web beans inject {@code SynchronizationPort}.
 * <p>
 * Tests must stub mocks via {@code @Autowired} fields; a {@code @MockitoBean} declared in a test class
 * creates a new context and defeats the sharing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@WebMvcTest
@ModuleSlicing(module = "notifications", extraIncludes = {"members", "sync"}, mode = ApplicationModuleTest.BootstrapMode.STANDALONE, verifyAutomatically = false)
@ActiveProfiles("test")
@Import(KlabisWebMvcSliceConfiguration.class)
@EnableConfigurationProperties(VapidProperties.class)
@NotificationsWebMvcMockitoBeans
@CommonWebMvcMockitoBeans
@MembersWebMvcMockitoBeans
@SyncWebMvcMockitoBeans
public @interface NotificationsWebMvcTest {
}
