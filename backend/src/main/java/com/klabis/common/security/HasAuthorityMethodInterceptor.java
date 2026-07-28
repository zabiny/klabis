package com.klabis.common.security;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.security.fieldsecurity.OwnerId;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.OwnershipResolver;
import com.klabis.common.security.fieldsecurity.SecuritySpelEvaluator;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
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

    /**
     * Matches methods annotated with {@link HasAuthority} or {@link OwnerVisible} on the class
     * itself, OR on any interface it implements — {@link org.springframework.aop.support.annotation.AnnotationMatchingPointcut}
     * only looks at the target class/method directly and misses annotations declared on a
     * generated OpenAPI {@code *Api} interface that the controller implements.
     */
    private static final Pointcut POINTCUT = new Pointcut() {
        @Override
        public ClassFilter getClassFilter() {
            return ClassFilter.TRUE;
        }

        @Override
        public MethodMatcher getMethodMatcher() {
            return new MethodMatcher() {
                @Override
                public boolean matches(Method method, Class<?> targetClass) {
                    return MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, HasAuthority.class) != null
                           || MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, OwnerVisible.class) != null
                           || MethodSecurityAnnotations.findClassAnnotation(targetClass, HasAuthority.class) != null;
                }

                @Override
                public boolean isRuntime() {
                    return false;
                }

                @Override
                public boolean matches(Method method, Class<?> targetClass, Object... args) {
                    throw new UnsupportedOperationException("Runtime matching not supported");
                }
            };
        }
    };

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Object target = invocation.getThis();
        Class<?> targetClass = target != null ? target.getClass() : method.getDeclaringClass();
        Authority requiredAuthority = resolveAuthority(method, targetClass);
        boolean isOwnerVisible = MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, OwnerVisible.class) != null;

        if (requiredAuthority == null && !isOwnerVisible) {
            return invocation.proceed();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean authorityGranted = requiredAuthority != null
                && SecuritySpelEvaluator.hasAuthority(authentication, requiredAuthority);

        if (authorityGranted) {
            return invocation.proceed();
        }

        if (isOwnerVisible && checkOwnership(method, targetClass, invocation.getArguments(), authentication)) {
            return invocation.proceed();
        }

        HandleAuthorizationDenied denied = resolveDeniedHandler(method, targetClass);
        if (denied != null) {
            MethodAuthorizationDeniedHandler handler = resolveHandler(denied.handlerClass());
            return handler.handleDeniedInvocation(invocation, DENY);
        }

        String denyReason = requiredAuthority != null
                ? "Access denied. Required authority: " + requiredAuthority.getValue()
                : "Access denied. Ownership required.";
        throw new AccessDeniedException(denyReason);
    }

    private boolean checkOwnership(Method method, Class<?> targetClass, Object[] arguments, Authentication authentication) {
        OwnershipResolver resolver = getOwnershipResolver();
        if (resolver == null) {
            return false;
        }
        int ownerIdIndex = MethodSecurityAnnotations.findAnnotatedParameterIndex(method, targetClass, OwnerId.class);
        if (ownerIdIndex < 0) {
            return false;
        }
        return resolver.isOwner(arguments[ownerIdIndex], authentication);
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



    private Authority resolveAuthority(Method method, Class<?> targetClass) {
        HasAuthority methodAnnotation = MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, HasAuthority.class);
        if (methodAnnotation != null) {
            return methodAnnotation.value();
        }

        HasAuthority classAnnotation = MethodSecurityAnnotations.findClassAnnotation(targetClass, HasAuthority.class);
        return classAnnotation != null ? classAnnotation.value() : null;
    }

    private HandleAuthorizationDenied resolveDeniedHandler(Method method, Class<?> targetClass) {
        HandleAuthorizationDenied methodLevel = MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, HandleAuthorizationDenied.class);
        if (methodLevel != null) {
            return methodLevel;
        }
        return MethodSecurityAnnotations.findClassAnnotation(targetClass, HandleAuthorizationDenied.class);
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
