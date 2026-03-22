package com.klabis.common.security;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.security.fieldsecurity.OwnerId;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
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
import java.lang.reflect.Parameter;

/**
 * Spring Security {@link AuthorizationAdvisor} that enforces {@link HasAuthority} annotations.
 * <p>
 * Replaces {@code HasAuthorityAspect}: same pointcut semantics (method annotation overrides
 * class annotation) but integrates with {@code AuthorizationAdvisorProxyFactory}, enabling
 * field-level authorization on response DTOs in addition to bean-level method security.
 * <p>
 * Also handles {@link OwnerVisible} methods: if the authority check fails but the method is
 * annotated with {@link OwnerVisible}, ownership is checked via the parameter annotated
 * with {@link OwnerId}. Access is granted if either the authority check or the ownership
 * check passes.
 * <p>
 * Registered as a {@code @Bean} so {@code AuthorizationAdvisorProxyFactory} auto-discovers it.
 */
public class HasAuthorityMethodInterceptor implements AuthorizationAdvisor, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private volatile OwnershipResolver ownershipResolver;

    private static final AuthorizationResult DENY = () -> false;

    private static final Pointcut POINTCUT = new ComposablePointcut(
            AnnotationMatchingPointcut.forClassAnnotation(HasAuthority.class))
            .union(AnnotationMatchingPointcut.forMethodAnnotation(HasAuthority.class))
            .union(AnnotationMatchingPointcut.forMethodAnnotation(OwnerVisible.class));

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Authority requiredAuthority = resolveAuthority(method, invocation.getThis());
        boolean isOwnerVisible = method.isAnnotationPresent(OwnerVisible.class);

        if (requiredAuthority == null && !isOwnerVisible) {
            return invocation.proceed();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean authorityGranted = requiredAuthority != null
                && SecuritySpelEvaluator.hasAuthority(authentication, requiredAuthority);

        if (authorityGranted) {
            return invocation.proceed();
        }

        if (isOwnerVisible && checkOwnership(method, invocation.getArguments(), authentication)) {
            return invocation.proceed();
        }

        HandleAuthorizationDenied denied = resolveDeniedHandler(method);
        if (denied != null) {
            MethodAuthorizationDeniedHandler handler = resolveHandler(denied.handlerClass());
            return handler.handleDeniedInvocation(invocation, DENY);
        }

        String denyReason = requiredAuthority != null
                ? "Access denied. Required authority: " + requiredAuthority.getValue()
                : "Access denied. Ownership required.";
        throw new AccessDeniedException(denyReason);
    }

    private boolean checkOwnership(Method method, Object[] arguments, Authentication authentication) {
        OwnershipResolver resolver = getOwnershipResolver();
        if (resolver == null) {
            return false;
        }
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(OwnerId.class)) {
                return resolver.isOwner(arguments[i], authentication);
            }
        }
        return false;
    }

    private OwnershipResolver getOwnershipResolver() {
        if (ownershipResolver == null && applicationContext != null) {
            try {
                ownershipResolver = applicationContext.getBean(OwnershipResolver.class);
            } catch (Exception ignored) {
            }
        }
        return ownershipResolver;
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

}
