package com.klabis.members.domain;

/**
 * Reason for membership deactivation.
 * <p>
 * This enum represents the various reasons why a member's membership
 * may be terminated or deactivated.
 */
public enum DeactivationReason {
    /**
     * Member submitted a resignation request (odhláška).
     */
    ODHLASKA,

    /**
     * Member transferred to another club (přestup).
     */
    PRESTUP,

    /**
     * Other reason with optional note.
     */
    OTHER
}
