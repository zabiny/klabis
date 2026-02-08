package com.klabis.users.authorization;

import com.klabis.users.Authority;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Annotation for method-level authorization checking.
 * <p>
 * Checks that the authenticated user has a specific global authority.
 * Works like {@link PreAuthorize} but provides type-safe authority checking.
 * <p>
 * <b>Usage:</b>
 * <pre>
 * &#64;HasAuthority(Authority.MEMBERS_CREATE)
 * &#64;PostMapping("/members")
 * public ResponseEntity&lt;?&gt; createMember(...) { ... }
 * </pre>
 * <p>
 * <b>Behavior:</b>
 * <ul>
 *   <li>Requires authentication (user must be logged in)</li>
 *   <li>Requires user to have the specified authority</li>
 *   <li>Throws {@code AccessDeniedException} if user lacks the authority</li>
 *   <li>Can be applied to class or method level</li>
 *   <li>Method-level annotation overrides class-level annotation</li>
 * </ul>
 * <p>
 * <b>Implementation:</b>
 * Uses Spring AOP aspect to intercept method calls and perform authorization checks
 * against the current authentication principal.
 *
 * @see Authority
 * @see HasAuthorityAspect
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HasAuthority {

    /**
     * The authority required to access the annotated method/class.
     *
     * @return the required authority
     */
    Authority value();
}
