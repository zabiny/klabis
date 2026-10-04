package com.klabis.common.ui;

import com.klabis.common.SecurityContextAuthorizationEvaluator;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Gives tests that run without a Spring test context a {@link HalFormsSupport} deciding over the authentication in
 * the {@code SecurityContextHolder}. Tests with a Spring context are served by
 * {@link HalFormsSupportInstanceTestExecutionListener}; without either, a static instance left behind by whichever
 * context started last would decide.
 */
public class PlainHalFormsSupportExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        if (!runsWithSpring(context.getRequiredTestClass())) {
            HalFormsSupport.useInstance(new HalFormsSupport(SecurityContextAuthorizationEvaluator.create()));
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (!runsWithSpring(context.getRequiredTestClass())) {
            HalFormsSupport.useInstance(null);
        }
    }

    private static boolean runsWithSpring(Class<?> testClass) {
        for (Class<?> type = testClass; type != null; type = type.getEnclosingClass()) {
            boolean spring = AnnotationSupport.findRepeatableAnnotations(type, ExtendWith.class).stream()
                    .flatMap(extendWith -> java.util.Arrays.stream(extendWith.value()))
                    .anyMatch(SpringExtension.class::equals);
            if (spring) {
                return true;
            }
        }
        return false;
    }
}
