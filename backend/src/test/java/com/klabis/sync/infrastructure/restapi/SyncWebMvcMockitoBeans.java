package com.klabis.sync.infrastructure.restapi;

import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncProjectionFieldReader;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mocks the ports required by the web beans of the sync module (synchronization controller and
 * {@code SyncStatePostprocessor}). {@link SynchronizationPort} is also injected by other modules' web
 * beans, hence tests must obtain it via {@code @Autowired}, never a second {@code @MockitoBean}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@MockitoBean(types = {
        SynchronizationPort.class,
        SyncProjectionFieldReader.class
})
public @interface SyncWebMvcMockitoBeans {
}
