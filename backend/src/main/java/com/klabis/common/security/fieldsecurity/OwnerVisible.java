package com.klabis.common.security.fieldsecurity;

import java.lang.annotation.*;

/**
 * Marks a field or method as accessible to the object's owner.
 * <p>
 * Combines with {@link com.klabis.common.users.HasAuthority} using OR semantics:
 * the field or method is accessible if the authority check passes <em>or</em> the
 * ownership check passes.
 * <p>
 * When used alone (without an authority annotation), the field or method is accessible
 * only to the owner.
 * <p>
 * Owner identity is decided by {@link com.klabis.common.authorization.AuthorizationEvaluator#isSelf}, which compares the
 * target (found via {@link com.klabis.common.authorization.TargetId}) with the user and member ID of the
 * JWT token.
 *
 * @see com.klabis.common.authorization.TargetId
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OwnerVisible {
}
