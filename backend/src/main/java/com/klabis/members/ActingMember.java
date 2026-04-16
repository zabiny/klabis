package com.klabis.members;

import io.swagger.v3.oas.annotations.Hidden;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for injecting the authenticated member's {@link MemberId} into controller parameters.
 * <p>
 * Unlike {@link ActingUser}, this annotation requires the authenticated user to have a member profile.
 * If the user has no member profile, {@link com.klabis.common.exceptions.MemberProfileRequiredException}
 * is thrown before the controller method executes.
 * <p>
 * Supported parameter types:
 * - {@link MemberId} - injects the authenticated member's ID
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Hidden // do not show in Springdoc generated API docs
public @interface ActingMember {
}
