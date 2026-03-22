package com.klabis.common.security;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.AuthorizationAdvisor;
import org.springframework.security.authorization.method.AuthorizationInterceptorsOrder;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.security.authorization.method.MethodAuthorizationDeniedHandler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

/**
 * Spring Security {@link AuthorizationAdvisor} that enforces {@link HasAuthority} annotations.
 * <p>
 * Replaces {@code HasAuthorityAspect}: same pointcut semantics (method annotation overrides
 * class annotation) but integrates with {@code AuthorizationAdvisorProxyFactory}, enabling
 * field-level authorization on response DTOs in addition to bean-level method security.
 * <p>
 * Registered as a {@code @Bean} so {@code AuthorizationAdvisorProxyFactory} auto-discovers it.
 */
public class HasAuthorityMethodInterceptor implements AuthorizationAdvisor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private static final Pointcut POINTCUT = new ComposablePointcut(
            AnnotationMatchingPointcut.forClassAnnotation(HasAuthority.class))
            .union(AnnotationMatchingPointcut.forMethodAnnotation(HasAuthority.class));

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Authority requiredAuthority = resolveAuthority(invocation.getMethod(), invocation.getThis());

        if (requiredAuthority == null) {
            return invocation.proceed();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!isAuthorized(authentication, requiredAuthority)) {
            HandleAuthorizationDenied denied = resolveDeniedHandler(invocation.getMethod());
            if (denied != null) {
                MethodAuthorizationDeniedHandler handler = resolveHandler(denied.handlerClass());
                return handler.handleDeniedInvocation(invocation, denyResult(requiredAuthority));
            }
            throw new AccessDeniedException("Access denied. Required authority: " + requiredAuthority.getValue());
        }

        return invocation.proceed();
    }

    @Override
    public Pointcut getPointcut() {
        return POINTCUT;
    }

    @Override
    public Advice getAdvice() {
        return this;
    }

    @Override
    public int getOrder() {
        return AuthorizationInterceptorsOrder.PRE_AUTHORIZE.getOrder() + 1;
    }

    private Authority resolveAuthority(Method method, Object target) {
        HasAuthority methodAnnotation = method.getAnnotation(HasAuthority.class);
        if (methodAnnotation != null) {
            return methodAnnotation.value();
        }

        if (target != null) {
            HasAuthority classAnnotation = target.getClass().getAnnotation(HasAuthority.class);
            if (classAnnotation != null) {
                return classAnnotation.value();
            }
        }

        // Class-level on the declaring interface/class
        HasAuthority declaringAnnotation = method.getDeclaringClass().getAnnotation(HasAuthority.class);
        return declaringAnnotation != null ? declaringAnnotation.value() : null;
    }

    private HandleAuthorizationDenied resolveDeniedHandler(Method method) {
        HandleAuthorizationDenied methodLevel = method.getAnnotation(HandleAuthorizationDenied.class);
        if (methodLevel != null) {
            return methodLevel;
        }
        return method.getDeclaringClass().getAnnotation(HandleAuthorizationDenied.class);
    }

    private boolean isAuthorized(Authentication authentication, Authority requiredAuthority) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String required = requiredAuthority.getValue();
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(required));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private MethodAuthorizationDeniedHandler resolveHandler(Class<? extends MethodAuthorizationDeniedHandler> handlerClass) {
        if (applicationContext != null) {
            try {
                return applicationContext.getBean(handlerClass);
            } catch (BeansException ignored) {
            }
        }
        try {
            return handlerClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve MethodAuthorizationDeniedHandler: " + handlerClass, e);
        }
    }

    private AuthorizationResult denyResult(Authority authority) {
        return () -> false;
    }
}
