package com.klabis.members;

import io.swagger.v3.oas.annotations.Hidden;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for injecting the authenticated user's identifiers into controller parameters.
 * <p>
 * Supported parameter types:
 * - {@link com.klabis.common.users.UserId} - injects the authenticated user's ID
 * - {@link CurrentUserData} - injects full user data including memberId (if available)
 * <p>
 * For controllers that require a member profile, use {@link ActingMember} instead —
 * it resolves {@link MemberId} directly and throws {@link com.klabis.common.exceptions.MemberProfileRequiredException}
 * when the user has no member profile.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Hidden // do not show in Springdoc generated API docs
public @interface ActingUser {
}
