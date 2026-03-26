package com.klabis.common.security.fieldsecurity;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SecuritySpelEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(SecuritySpelEvaluator.class);

    private static final DefaultMethodSecurityExpressionHandler HANDLER = new DefaultMethodSecurityExpressionHandler();
    private static final ConcurrentMap<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

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
            Expression expr = EXPRESSION_CACHE.computeIfAbsent(expression,
                    e -> HANDLER.getExpressionParser().parseExpression(e));
            Boolean result = expr.getValue(evalContext, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            LOG.debug("Failed to evaluate @PreAuthorize expression '{}' on method {}: {}", expression, method.getName(), e.getMessage());
            return false;
        }
    }

    public static boolean hasAuthority(@Nullable Authentication authentication, Authority required) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String value = required.getValue();
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(value));
    }

    public static boolean isFieldAuthorized(
            @Nullable PreAuthorize preAuthorize,
            @Nullable HasAuthority hasAuthority,
            boolean ownerVisible,
            Method accessorMethod,
            @Nullable Object ownerIdValue,
            @Nullable Authentication authentication,
            @Nullable OwnershipResolver ownershipResolver) {

        if (preAuthorize != null && evaluate(preAuthorize.value(), accessorMethod, authentication)) {
            return true;
        }
        if (hasAuthority != null && hasAuthority(authentication, hasAuthority.value())) {
            return true;
        }
        if (ownerVisible && ownershipResolver != null && ownerIdValue != null) {
            return ownershipResolver.isOwner(ownerIdValue, authentication);
        }
        return preAuthorize == null && hasAuthority == null && !ownerVisible;
    }
}
