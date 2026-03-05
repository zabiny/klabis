package com.klabis.members;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for injecting the authenticated user's identifiers into controller parameters.
 * <p>
 * Use this annotation on controller method parameters to automatically inject
 * UserId without manual SecurityContextHolder access.
 * <p>
 * Example usage:
 * <pre>{@code
 * @PostMapping("/events/{eventId}/register")
 * public ResponseEntity<Void> registerForEvent(
 *         @PathVariable UUID eventId,
 *         @CurrentUser UserId userId) {
 *     eventRegistrationService.registerMember(eventId, userId, command);
 *     return ResponseEntity.noContent().build();
 * }
 * }</pre>
 * <p>
 * Supported parameter types:
 * - {@link com.klabis.common.users.UserId} - injects the authenticated user's ID
 * - {@link CurrentUserData} - injects user data including memberId (if available)
 * <p>
 * Note: MemberId is accessible via {@link CurrentUserData#memberId()} when the user has a Member profile.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
