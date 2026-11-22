package com.klabis.common.authorization;

import java.lang.annotation.*;

/**
 * Marks the method parameter or record component that identifies the target an operation or field is about.
 * <p>
 * Authorities listed in {@link com.klabis.common.users.HasAuthority} may then be held over just that target,
 * not only over everything. An operation or record may identify one target per {@link TargetType}; each authority
 * is then checked over the target of its own kind.
 * <p>
 * The value may be a UUID or any type convertible to one through {@code ConversionService}.
 *
 * @see AuthorizationEvaluator#canInvoke
 */
@Target({ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TargetId {

    TargetType value();
}
