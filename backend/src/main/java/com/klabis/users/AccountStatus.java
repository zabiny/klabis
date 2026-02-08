package com.klabis.users;

/**
 * Account status for user accounts.
 * <p>
 * - ACTIVE: User can authenticate and access the system
 * - PENDING_ACTIVATION: User created but must set password (not yet activated)
 * - SUSPENDED: User account is suspended (cannot authenticate)
 */
public enum AccountStatus {
    ACTIVE,
    PENDING_ACTIVATION,
    SUSPENDED
}
