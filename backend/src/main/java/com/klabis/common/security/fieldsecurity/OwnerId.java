package com.klabis.common.security.fieldsecurity;

import java.lang.annotation.*;

/**
 * Marks which record component or method parameter carries the owner identifier.
 * <p>
 * On record components: identifies the owner field used during response serialization
 * to determine whether the current user is the owner of the object being serialized.
 * <p>
 * On method parameters: identifies the owner value for method-level and request body
 * authorization checks.
 * <p>
 * Automatic owner field discovery: if a record has exactly one field whose type can be
 * converted to UUID via {@code ConversionService}, that field is used automatically.
 * Use {@code @OwnerId} explicitly when multiple such candidates exist.
 *
 * Equivalent to {@code @TargetId(TargetType.MEMBER)}; kept until the generated API code is switched to
 * {@link com.klabis.common.authorization.TargetId}.
 *
 * @see OwnerVisible
 * @see OwnershipResolver
 */
@Target({ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OwnerId {
}
