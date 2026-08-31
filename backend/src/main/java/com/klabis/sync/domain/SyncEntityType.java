package com.klabis.sync.domain;

/**
 * Which kind of Klabis entity a synchronisation record refers to.
 * <p>
 * Each value declares the REST path segment used to address it under
 * {@code /api/{entityType}/{id}/sync…} (design.md D14) — declared here, not derived
 * from the enum name, so the mapping is explicit and reviewable.
 */
public enum SyncEntityType {

    EVENT("events");

    private final String pathSegment;

    SyncEntityType(String pathSegment) {
        this.pathSegment = pathSegment;
    }

    public String pathSegment() {
        return pathSegment;
    }
}
