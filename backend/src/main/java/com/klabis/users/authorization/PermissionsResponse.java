package com.klabis.users.authorization;

import com.klabis.users.UserId;

import java.util.List;

/**
 * Response DTO containing user permissions.
 *
 * @param userId      the user ID
 * @param authorities list of authorities granted to the user
 */
public record PermissionsResponse(UserId userId, List<String> authorities) {
}
