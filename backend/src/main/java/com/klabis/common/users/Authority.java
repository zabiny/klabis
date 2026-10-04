package com.klabis.common.users;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.common.authorization.GrantForm;
import com.klabis.common.authorization.TargetType;

import java.util.Arrays;
import java.util.Collections;
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
    CALENDAR_MANAGE("CALENDAR:MANAGE", TargetType.NONE, GrantForm.ALL),
    MEMBERS_MANAGE("MEMBERS:MANAGE", TargetType.MEMBER, GrantForm.ALL),
    MEMBERS_READ("MEMBERS:READ", TargetType.MEMBER, GrantForm.ALL),
    MEMBERS_PERMISSIONS("MEMBERS:PERMISSIONS", TargetType.MEMBER, GrantForm.ALL),
    MEMBERS_EDIT_PROFILE("MEMBERS:EDIT_PROFILE", TargetType.MEMBER, GrantForm.SPECIFIC),
    EVENTS_READ("EVENTS:READ", TargetType.EVENT, GrantForm.ALL),
    EVENTS_MANAGE("EVENTS:MANAGE", TargetType.EVENT, GrantForm.ALL),
    EVENTS_REGISTRATIONS("EVENTS:REGISTRATIONS", TargetType.MEMBER, GrantForm.ALL),
    GROUPS_TRAINING("GROUPS:TRAINING", TargetType.NONE, GrantForm.ALL),
    FINANCE_MANAGE("FINANCE:MANAGE", TargetType.NONE, GrantForm.ALL),
    SYNC_MANAGE("SYNC:MANAGE", TargetType.NONE, GrantForm.ALL),
    DEVELOPER("DEVELOPER", TargetType.NONE, GrantForm.ALL);

    public static final String CALENDAR_SCOPE = "CALENDAR";
    public static final String MEMBERS_SCOPE = "MEMBERS";
    public static final String EVENTS_SCOPE = "EVENTS";
    public static final String GROUPS_SCOPE = "GROUPS";
    public static final String FINANCE_SCOPE = "FINANCE";
    public static final String SYNC_SCOPE = "SYNC";

    private final String value;
    private final TargetType targetType;
    private final Set<GrantForm> grantForms;

    Authority(String value, TargetType targetType, GrantForm... grantForms) {
        this.value = value;
        this.targetType = targetType;
        this.grantForms = EnumSet.copyOf(Arrays.asList(grantForms));
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public Set<GrantForm> getGrantForms() {
        return Collections.unmodifiableSet(grantForms);
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

    public static Set<Authority> withStandard(Set<Authority> authorities) {
        var merged = EnumSet.copyOf(getStandardUserAuthorities());
        merged.addAll(authorities);
        return merged;
    }

    public static Set<Authority> withoutStandard(Set<Authority> authorities) {
        var filtered = authorities.isEmpty() ? EnumSet.noneOf(Authority.class) : EnumSet.copyOf(authorities);
        filtered.removeAll(getStandardUserAuthorities());
        return filtered;
    }

    /**
     * The authorities that may be granted over everything through the permissions API: every authority
     * holdable in {@link GrantForm#ALL} except the standard user authorities (held by every user) and
     * the internal-only {@link #DEVELOPER}. Used to offer the assignable catalogue in HAL-FORMS options —
     * not the target user's current authorities.
     */
    public static Set<Authority> assignableAuthorities() {
        EnumSet<Authority> assignable = EnumSet.copyOf(grantableOverAll());
        assignable.removeAll(getStandardUserAuthorities());
        assignable.remove(DEVELOPER);
        return assignable;
    }

    /**
     * Every authority that may be held over everything ({@link GrantForm#ALL}), i.e. may be a user's direct
     * authority, including the standard user authorities and {@link #DEVELOPER}.
     */
    public static Set<Authority> grantableOverAll() {
        EnumSet<Authority> grantable = EnumSet.noneOf(Authority.class);
        for (Authority authority : values()) {
            if (authority.grantForms.contains(GrantForm.ALL)) {
                grantable.add(authority);
            }
        }
        return grantable;
    }

    /**
     * Authorities that may be held over specific targets, i.e. derived from relationships. Administrator
     * authorities are {@link GrantForm#ALL}-only and therefore never delegatable.
     */
    public static Set<Authority> delegatable() {
        EnumSet<Authority> delegatable = EnumSet.noneOf(Authority.class);
        for (Authority authority : values()) {
            if (authority.grantForms.contains(GrantForm.SPECIFIC)) {
                delegatable.add(authority);
            }
        }
        return delegatable;
    }

    public static boolean isKnownAuthority(String value) {
        for (Authority authority : values()) {
            if (authority.value.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
