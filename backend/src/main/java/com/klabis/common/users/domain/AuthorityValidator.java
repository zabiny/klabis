package com.klabis.common.users.domain;

import com.klabis.common.users.Authority;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for validating user authorities.
 * <p>
 * Provides centralized validation logic for authority sets, ensuring:
 * <ul>
 *   <li>Authorities are not null</li>
 *   <li>All authorities are from the valid enum values</li>
 * </ul>
 * An empty set is valid — a user may hold no authorities.
 * <p>
 * This validator enforces the authority-based access control model where
 * all authorities must match the Authority enum values.
 * <p>
 * Uses the strongly-typed Authority enum for validation, providing compile-time safety
 * and preventing typos in authority strings.
 */
public final class AuthorityValidator {

    // Private constructor to prevent instantiation
    private AuthorityValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates a set of authorities.
     * <p>
     * Ensures that the authorities set is not null and contains
     * only valid authorities that can be converted to the Authority enum.
     * An empty set is valid — a user may hold no authorities.
     *
     * @param authorities the set of authorities to validate
     * @throws IllegalArgumentException if authorities are null or contain invalid values
     */
    public static void validate(Set<String> authorities) {
        if (authorities == null) {
            throw new IllegalArgumentException("Authorities required");
        }

        // Validate each authority by attempting to convert to enum
        for (String authority : authorities) {
            try {
                Authority.fromString(authority);
            } catch (IllegalArgumentException e) {
                // Build a helpful error message with all valid authorities
                String validAuthorities = Set.of(Authority.values()).stream()
                        .map(Authority::getValue)
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException(
                        "Invalid authority: " + authority + ". Valid authorities: " + validAuthorities
                );
            }
        }
    }

    /**
     * Validates a set of Authority enums.
     * <p>
     * Ensures that the authorities set is not null. An empty set is valid — a user
     * may hold no authorities. Since the parameter is already typed as Authority enum,
     * no further validation is needed.
     *
     * @param authorities the set of authority enums to validate
     * @throws IllegalArgumentException if authorities are null
     */
    public static void validateAuthorityEnums(Set<Authority> authorities) {
        if (authorities == null) {
            throw new IllegalArgumentException("Authorities required");
        }
    }
}
