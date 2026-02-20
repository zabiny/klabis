package com.klabis.members.domain;

/**
 * Enum representing types of documents in the system.
 * <p>
 * This enum provides type-safe identification of different document types
 * that can be represented by the {@link ExpiringDocument} value object.
 */
public enum DocumentType {
    /**
     * Identity card document type.
     */
    IDENTITY_CARD("Identity card"),

    /**
     * Trainer license document type.
     */
    TRAINER_LICENSE("Trainer license");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this document type.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
