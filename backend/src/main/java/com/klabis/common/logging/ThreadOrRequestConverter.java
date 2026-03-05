package com.klabis.common.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.MDC;

/**
 * Custom Logback converter that outputs HTTP request context or thread name.
 *
 * <p>If {@code requestContext} is present in MDC (set during API request processing),
 * returns that value (e.g., "GET /members/123").
 *
 * <p>If {@code requestContext} is not present (non-API threads, startup logs, etc.),
 * returns the current thread name instead.
 *
 * <p>Output is wrapped in brackets without padding. For example: "[GET /members]"
 * or "[http-nio-8080-exec-1]".
 */
public class ThreadOrRequestConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String requestContext = MDC.get("requestContext");

        String value;
        if (requestContext != null && !requestContext.isEmpty()) {
            value = requestContext;
        } else {
            value = event.getThreadName();
        }

        // Wrap in brackets without padding
        return "[" + value + "]";
    }
}
