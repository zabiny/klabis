package com.klabis.members.domain;

/**
 * A detail Klabis requires of a complete {@link Member} that a given member currently lacks.
 * <p>
 * See {@link Member#missingData()} for how each item is derived and design.md D3 of the
 * {@code import-incomplete-members} change for the business rationale.
 */
public enum MissingDataItem {
    EMAIL,
    PHONE,
    BIRTH_NUMBER,
    GUARDIAN,
    ADDRESS
}
