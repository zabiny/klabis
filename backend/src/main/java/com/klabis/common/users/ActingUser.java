package com.klabis.common.users;

import io.swagger.v3.oas.annotations.Hidden;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated user's identity into a controller parameter.
 * <p>
 * Supported parameter types:
 * <ul>
 *   <li>{@link UserId} - the authenticated user's ID</li>
 *   <li>{@code com.klabis.members.CurrentUserData} - full user data, including the member id
 *   when the user has a member profile (resolved by the members module, which owns that type)</li>
 * </ul>
 * For controllers that require a member profile, use
 * {@code com.klabis.members.ActingMember} instead — it resolves the member id directly and throws
 * {@code MemberProfileRequiredException} when the user has no member profile.
 * <p>
 * Lives in {@code common.users} rather than {@code members} because it is generic identity
 * plumbing over {@link UserId}, which belongs to {@code users} — a {@code common} module that
 * every module may depend on. Keeping it here lets the {@code sync} engine inject the acting
 * user (as a {@link UserId}) without depending on {@code members}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Hidden // do not show in Springdoc generated API docs
public @interface ActingUser {
}
