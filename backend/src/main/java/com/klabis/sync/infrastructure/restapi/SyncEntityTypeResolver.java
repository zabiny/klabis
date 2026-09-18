package com.klabis.sync.infrastructure.restapi;

import com.klabis.sync.domain.SyncEntityType;

/**
 * Translates the generated {@link SyncEntityTypeParam} — bound from the
 * {@code {entityType}} path segment by Spring MVC itself, since the spec declares it
 * as an enumerated schema (`SyncEntityTypeParam`) rather than a free-form string — to
 * the domain's {@link SyncEntityType}, matching by {@link SyncEntityType#pathSegment()}
 * against {@link SyncEntityTypeParam#getValue()}, never by enum constant name: the two
 * enums' constant names differ on purpose (domain {@code EVENT} vs. generated
 * {@code EVENTS}), only their wire value ({@code "events"}) is the shared contract
 * (design.md D14: the mapping is declared, not derived from a name).
 * <p>
 * No unknown-value branch here: an {@code {entityType}} segment the spec's enum does
 * not list never reaches this method at all — {@code SyncEntityTypeParam}'s own
 * {@code @PathVariable} binding rejects it first (Spring throws
 * {@code MethodArgumentTypeMismatchException} before any handler runs, satisfying
 * design.md D14's "before any handler runs" literally, unlike the routing-level
 * mechanism D14 otherwise describes). {@link SyncExceptionHandler} maps that binding
 * failure to 404, matching the "entity not enrolled" shape callers already expect from
 * every other 404 case on these endpoints.
 */
final class SyncEntityTypeResolver {

    private SyncEntityTypeResolver() {
    }

    static SyncEntityType resolve(SyncEntityTypeParam pathValue) {
        for (SyncEntityType entityType : SyncEntityType.values()) {
            if (entityType.pathSegment().equals(pathValue.getValue())) {
                return entityType;
            }
        }
        // Unreachable in practice: every SyncEntityTypeParam constant the spec
        // enumerates has a matching SyncEntityType, and the binding step above already
        // refuses any value neither enum lists. Kept as a defined failure mode rather
        // than an unchecked one, in case the two enums ever drift apart.
        throw new IllegalStateException("No SyncEntityType declares path segment " + pathValue.getValue());
    }
}
