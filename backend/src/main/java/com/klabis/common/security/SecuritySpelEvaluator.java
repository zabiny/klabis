package com.klabis.common.security;

import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.core.Authentication;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

public class SecuritySpelEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(SecuritySpelEvaluator.class);

    private static final DefaultMethodSecurityExpressionHandler HANDLER = new DefaultMethodSecurityExpressionHandler();

    private SecuritySpelEvaluator() {
    }

    public static boolean evaluate(String expression, Method method, @Nullable Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        try {
            Object dummyTarget = new Object();
            MethodInvocation dummyInvocation = new MethodInvocation() {
                @Override public Method getMethod() { return method; }
                @Override public Object[] getArguments() { return new Object[0]; }
                @Override public Object getThis() { return dummyTarget; }
                @Override public AccessibleObject getStaticPart() { return method; }
                @Override public Object proceed() { return null; }
            };
            var evalContext = HANDLER.createEvaluationContext(authentication, dummyInvocation);
            Boolean result = HANDLER.getExpressionParser()
                    .parseExpression(expression)
                    .getValue(evalContext, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            LOG.debug("Failed to evaluate @PreAuthorize expression '{}' on method {}: {}", expression, method.getName(), e.getMessage());
            return false;
        }
    }
}
