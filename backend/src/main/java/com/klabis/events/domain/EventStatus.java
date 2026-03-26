package com.klabis.events.domain;

import java.util.EnumSet;
import java.util.Map;

/**
 * Enum representing the lifecycle status of an event.
 * <p>
 * Status transition rules:
 * <ul>
 *   <li>DRAFT → ACTIVE, CANCELLED</li>
 *   <li>ACTIVE → CANCELLED, FINISHED</li>
 *   <li>FINISHED → (none)</li>
 *   <li>CANCELLED → (none)</li>
 *   <li>Same status → allowed (idempotent)</li>
 * </ul>
 */
public enum EventStatus {
    /**
     * Event is in draft state, not yet published.
     */
    DRAFT,

    /**
     * Event is active and accepting registrations.
     */
    ACTIVE,

    /**
     * Event has finished.
     */
    FINISHED,

    /**
     * Event has been cancelled.
     */
    CANCELLED;

    // Define allowed transitions for each status
    private static final Map<EventStatus, EnumSet<EventStatus>> ALLOWED_TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(DRAFT, ACTIVE, CANCELLED),
            ACTIVE, EnumSet.of(ACTIVE, CANCELLED, FINISHED),
            FINISHED, EnumSet.of(FINISHED),
            CANCELLED, EnumSet.of(CANCELLED)
    );

    /**
     * Checks if transition from current status to target status is allowed.
     *
     * @param target the target status to transition to
     * @return true if transition is allowed, false otherwise
     */
    public boolean canTransitionTo(EventStatus target) {
        if (target == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    /**
     * Validates that transition from current status to new status is allowed.
     *
     * @param newStatus the new status to transition to
     * @throws IllegalArgumentException if newStatus is null
     * @throws IllegalStateException    if transition is not allowed
     */
    public void validateTransition(EventStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }

        if (!canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition from %s to %s".formatted(this, newStatus)
            );
        }
    }
}
