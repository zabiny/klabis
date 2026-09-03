package com.klabis.sync.application;

import com.klabis.sync.domain.SyncEntityType;

/**
 * More than one external system has a registered adapter for one
 * {@link com.klabis.sync.domain.SyncEntityType} — {@link SynchronizationPort#findByTarget}
 * cannot resolve a single record from the entity type and id alone. A configuration
 * error (this change's API has no way to disambiguate at the call site), not a normal
 * runtime state.
 * <p>
 * <b>Deliberately left unmapped by {@code SyncExceptionHandler}</b>, so it falls through
 * to Spring's default handling as a plain {@code 500 Internal Server Error}. No caller
 * input produces this — it can only happen if two adapters are registered for the same
 * entity type, something only a deployment misconfiguration can cause — so there is no
 * client-actionable status to map it to (unlike the sibling {@code 409} exceptions in
 * {@code SyncExceptionHandler}, which all mean "a manager must decide something"). 500 is
 * therefore the intended, permanent outcome, not an oversight; do not add a handler for
 * this exception without revisiting this reasoning.
 */
public class AmbiguousSyncTargetException extends RuntimeException {

    public AmbiguousSyncTargetException(SyncEntityType entityType, int adapterCount) {
        super("Cannot resolve a synchronisation record for entity type " + entityType + " by target alone: "
                + adapterCount + " external systems have a registered adapter for it");
    }
}
