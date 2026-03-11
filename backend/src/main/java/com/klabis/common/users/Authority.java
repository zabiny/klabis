package com.klabis.common.users;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.EnumSet;
import java.util.Set;

/**
 * Enum representing all valid user authorities in the system.
 * <p>
 * Provides type-safe authority references throughout the codebase with compile-time checking.
 * Each enum constant has a corresponding string value used for storage and JWT claims.
 * <p>
 * The enum names use underscore pattern (MEMBERS_MANAGE) following Java conventions,
 * while getValue() returns the colon-separated string format (MEMBERS:MANAGE) for
 * external representation.
 * <p>
 * Benefits:
 * <ul>
 *   <li>Type safety: Compile-time checking of authority references</li>
 *   <li>Refactoring: IDE can find all usages of an authority</li>
 *   <li>Validation: Enum prevents typos in authority strings</li>
 *   <li>Documentation: Single source of truth for all authorities</li>
 * </ul>
 */
public enum Authority {
    CALENDAR_MANAGE("CALENDAR:MANAGE", Scope.CONTEXT_SPECIFIC),
    MEMBERS_MANAGE("MEMBERS:MANAGE", Scope.CONTEXT_SPECIFIC),
    MEMBERS_READ("MEMBERS:READ", Scope.CONTEXT_SPECIFIC),
    MEMBERS_PERMISSIONS("MEMBERS:PERMISSIONS", Scope.GLOBAL),
    EVENTS_READ("EVENTS:READ", Scope.GLOBAL),
    EVENTS_MANAGE("EVENTS:MANAGE", Scope.CONTEXT_SPECIFIC);

    public static final String CALENDAR_SCOPE = "CALENDAR";
    public static final String MEMBERS_SCOPE = "MEMBERS";
    public static final String EVENTS_SCOPE = "EVENTS";

    private final String value;
    private final Scope scope;

    Authority(String value, Scope scope) {
        this.value = value;
        this.scope = scope;
    }

    /**
     * Scope classification for authorities.
     * <p>
     * GLOBAL: Cannot be granted via groups (admin-level permissions)
     * CONTEXT_SPECIFIC: Can be granted via groups in future group-based authorization
     */
    public enum Scope {
        GLOBAL,
        CONTEXT_SPECIFIC
    }

    /**
     * Gets the scope classification for this authority.
     *
     * @return the scope (GLOBAL or CONTEXT_SPECIFIC)
     */
    public Scope getScope() {
        return scope;
    }

    /**
     * Gets the string representation of this authority.
     * <p>
     * Used for JWT claims, database storage, and Spring Security's GrantedAuthority.
     *
     * @return the colon-separated string representation (e.g., "MEMBERS:MANAGE")
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Converts a string value to the corresponding Authority enum.
     * <p>
     * Used for JPA/database conversion and JWT claims parsing.
     *
     * @param value the string representation (e.g., "MEMBERS:MANAGE")
     * @return the corresponding Authority enum
     * @throws IllegalArgumentException if the value is not a valid authority
     */
    @JsonCreator
    public static Authority fromString(String value) {
        for (Authority authority : values()) {
            if (authority.value.equals(value)) {
                return authority;
            }
        }
        throw new IllegalArgumentException("Unknown authority: " + value);
    }

    @Override
    public String toString() {
        return value;
    }

    public static Set<Authority> getStandardUserAuthorities() {
        return EnumSet.of(MEMBERS_READ, EVENTS_READ);
    }
}
