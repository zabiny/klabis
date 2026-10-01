package com.klabis.common;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Registered globally (junit-platform.properties + META-INF/services) so that a test setting
 * {@link SecurityContextHolder} by hand cannot leak its authentication into tests running later on the same thread.
 */
public class ClearSecurityContextExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) {
        SecurityContextHolder.clearContext();
    }
}
