package com.klabis.common.security;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
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
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Spring Security {@link AuthorizationAdvisor} that enforces {@link HasAuthority} annotations.
 * <p>
 * Replaces {@code HasAuthorityAspect}: same pointcut semantics (method annotation overrides
 * class annotation) but integrates with {@code AuthorizationAdvisorProxyFactory}, enabling
 * field-level authorization on response DTOs in addition to bean-level method security.
 * <p>
 * The decision itself is made by {@link AuthorizationEvaluator#canInvoke}, which also covers
 * {@link OwnerVisible} methods and authorities held over just the target identified by a
 * {@link com.klabis.common.authorization.TargetId} parameter.
 * <p>
 * Registered as a {@code @Bean} so {@code AuthorizationAdvisorProxyFactory} auto-discovers it.
 */
public class HasAuthorityMethodInterceptor implements AuthorizationAdvisor, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private volatile AuthorizationEvaluator evaluator;

    private static final AuthorizationResult DENY = () -> false;

    private static final String APPLICATION_PACKAGE_PREFIX = "com.klabis.";

    /**
     * Matches methods annotated with {@link HasAuthority} or {@link OwnerVisible} on the class
     * itself, OR on any interface it implements — {@link org.springframework.aop.support.annotation.AnnotationMatchingPointcut}
     * only looks at the target class/method directly and misses annotations declared on a
     * generated OpenAPI {@code *Api} interface that the controller implements.
     * <p>
     * The class filter limits matching to application classes: the interface-aware lookup is
     * expensive and running it for every method of every framework bean dominated context startup.
     */
    private static final Pointcut POINTCUT = new Pointcut() {
        @Override
        public ClassFilter getClassFilter() {
            return clazz -> clazz.getName().startsWith(APPLICATION_PACKAGE_PREFIX);
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

        if (evaluator().canInvoke(method, targetClass, invocation.getArguments())) {
            return invocation.proceed();
        }

        HandleAuthorizationDenied denied = resolveDeniedHandler(method, targetClass);
        if (denied != null) {
            MethodAuthorizationDeniedHandler handler = resolveHandler(denied.handlerClass());
            return handler.handleDeniedInvocation(invocation, DENY);
        }

        throw new AccessDeniedException(denyReason(method, targetClass));
    }

    private String denyReason(Method method, Class<?> targetClass) {
        HasAuthority required = resolveHasAuthority(method, targetClass);
        if (required == null) {
            return "Access denied. Ownership required.";
        }
        return "Access denied. Required authority: " + Arrays.stream(required.value())
                .map(Authority::getValue)
                .collect(Collectors.joining(" or "));
    }

    private AuthorizationEvaluator evaluator() {
        if (evaluator == null) {
            Assert.state(applicationContext != null, "HasAuthorityMethodInterceptor is not attached to an application context");
            evaluator = applicationContext.getBean(AuthorizationEvaluator.class);
        }
        return evaluator;
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



    private HasAuthority resolveHasAuthority(Method method, Class<?> targetClass) {
        HasAuthority methodAnnotation = MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, HasAuthority.class);
        return methodAnnotation != null ? methodAnnotation : MethodSecurityAnnotations.findClassAnnotation(targetClass, HasAuthority.class);
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
