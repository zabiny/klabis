package com.klabis.users.authorization;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.users.Authority;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AspectJ advice for {@link HasAuthority} annotation processing.
 * <p>
 * Intercepts method calls annotated with @HasAuthority and performs
 * authorization checks before method execution.
 * <p>
 * Authorization Logic:
 * <ol>
 *   <li>Retrieves the required authority from the annotation</li>
 *   <li>Gets the current authentication from SecurityContext</li>
 *   <li>Checks if user is authenticated and has the required authority</li>
 *   <li>Throws AccessDeniedException if authorization fails</li>
 * </ol>
 * <p>
 * Method Resolution:
 * <ul>
 *   <li>Checks method-level annotation first (takes precedence)</li>
 *   <li>Falls back to class-level annotation if method has none</li>
 *   <li>Returns null (skip check) if no annotation found</li>
 * </ul>
 *
 * @see HasAuthority
 */
@Aspect
@Component
@MvcComponent
class HasAuthorityAspect {

    private static final Logger log = LoggerFactory.getLogger(HasAuthorityAspect.class);

    @Before("@annotation(HasAuthority) || @within(HasAuthority)")
    public void checkAuthority(JoinPoint joinPoint) {
        Authority requiredAuthority = resolveAuthority(joinPoint);

        if (requiredAuthority == null) {
            // Should not happen if annotation is properly used
            log.warn("No @HasAuthority annotation found on method: {}", joinPoint.getSignature());
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!isAuthorized(authentication, requiredAuthority)) {
            String user = authentication != null ? authentication.getName() : "ANONYMOUS";
            log.debug("Authorization check failed for user: {} (required authority: {})", user, requiredAuthority);
            throw new AccessDeniedException("Access denied. Required authority: " + requiredAuthority.getValue());
        }

        log.debug("Authorization check passed for user: {} (required authority: {})",
                authentication != null ? authentication.getName() : "ANONYMOUS",
                requiredAuthority);
    }

    /**
     * Resolves the required authority from the annotation.
     * <p>
     * Checks method-level annotation first, then falls back to class-level.
     *
     * @param joinPoint the join point (method being called)
     * @return the required authority, or null if no annotation found
     */
    private Authority resolveAuthority(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Check method-level annotation first
        HasAuthority methodAnnotation = method.getAnnotation(HasAuthority.class);
        if (methodAnnotation != null) {
            return methodAnnotation.value();
        }

        // Fall back to class-level annotation
        Class<?> targetClass = joinPoint.getTarget().getClass();
        HasAuthority classAnnotation = targetClass.getAnnotation(HasAuthority.class);
        if (classAnnotation != null) {
            return classAnnotation.value();
        }

        return null;
    }

    /**
     * Checks if the authentication principal has the required authority.
     *
     * @param authentication    the current authentication (may be null)
     * @param requiredAuthority the required authority
     * @return true if user is authenticated and has the authority, false otherwise
     */
    private boolean isAuthorized(Authentication authentication, Authority requiredAuthority) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String requiredAuthorityValue = requiredAuthority.getValue();

        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(requiredAuthorityValue));
    }
}
