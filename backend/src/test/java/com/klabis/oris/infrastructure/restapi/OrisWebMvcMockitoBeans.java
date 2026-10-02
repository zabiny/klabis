package com.klabis.oris.infrastructure.restapi;

import com.klabis.oris.application.ImportedOrisEventsPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mocks the always-available primary ports required by the web beans of the oris module.
 * <p>
 * {@code OrisApiClient} is deliberately not mocked here: it is an external client injected directly into the
 * profile-gated {@code OrisController} (documented exception to "mock only primary ports"), so the test that
 * needs it declares it locally.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@MockitoBean(types = ImportedOrisEventsPort.class)
public @interface OrisWebMvcMockitoBeans {
}
