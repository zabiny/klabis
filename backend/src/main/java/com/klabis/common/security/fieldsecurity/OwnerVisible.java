package com.klabis.common.security.fieldsecurity;

import com.klabis.common.users.HasAuthority;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Marks a field or method as accessible to the object's owner.
 * <p>
 * Combines with {@link HasAuthority} and {@link PreAuthorize} using OR semantics:
 * the field or method is accessible if the authority check passes <em>or</em> the
 * ownership check passes.
 * <p>
 * When used alone (without an authority annotation), the field or method is accessible
 * only to the owner.
 * <p>
 * Owner identity is resolved by {@link OwnershipResolver}, which compares the owner
 * identifier (found via {@link OwnerId}) against the current user's member ID from
 * the JWT token.
 *
 * @see OwnerId
 * @see OwnershipResolver
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OwnerVisible {
}
