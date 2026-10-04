package com.klabis.common.authorization;

import java.lang.annotation.*;

/**
 * Marks the method parameter or record component that identifies the target an operation or field is about.
 * <p>
 * Authorities listed in {@link com.klabis.common.users.HasAuthority} may then be held over just that target,
 * not only over everything, and {@link com.klabis.common.security.fieldsecurity.OwnerVisible} compares the
 * target with the authenticated user.
 * <p>
 * The value may be a UUID or any type convertible to one through {@code ConversionService}.
 * <p>
 * On a record, the component may be left unmarked when it is the only one convertible to a UUID and some
 * component is {@code @OwnerVisible}; the target is then taken to be a member.
 *
 * @see AuthorizationEvaluator#canInvoke
 */
@Target({ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetId {

    TargetType value();
}
