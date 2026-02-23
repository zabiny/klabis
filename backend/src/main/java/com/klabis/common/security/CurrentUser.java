package com.klabis.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for injecting the authenticated user's identifiers into controller parameters.
 * <p>
 * Use this annotation on controller method parameters to automatically inject
 * UserId or MemberId UUID without manual SecurityContextHolder access.
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
 * - {@link java.util.UUID} - injects the member's ID UUID (throws if user has no Member profile)
 * <p>
 * Note: For type-safe MemberId injection in members module, use {@code MemberId.fromUserId(userId)}
 * or convert the UUID manually. This design avoids module dependency from common to members.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
