package com.klabis.members.infrastructure.orissync;

/**
 * Holds the ORIS club key without ever disclosing it (design.md D9). Deliberately
 * has no getter — the value can only be replaced or discarded, never read back, so
 * no caller outside this package can leak it even by accident.
 */
interface OrisClubKeyPort {

    void store(String clubKey);

    boolean isSet();

    void clear();
}
