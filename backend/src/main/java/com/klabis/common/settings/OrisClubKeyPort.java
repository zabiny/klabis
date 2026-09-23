package com.klabis.common.settings;

/**
 * Holds a shared secret setting without ever disclosing it (design.md D9). Deliberately
 * has no getter — the value can only be replaced or discarded, never read back through
 * this type, so no caller can leak it even by accident.
 * <p>
 * The read side lives on {@link OrisClubKeyAccessor}, which a caller that genuinely needs
 * to attach the secret to an outgoing request injects explicitly. The separation keeps the
 * common case — "is it set, replace it, discard it" — free of any way to read the value.
 * <p>
 * Shared from {@code common.settings} because the concept (a cluster-wide secret held only
 * in memory) is not specific to ORIS; other modules can hold their own secrets behind the
 * same contract.
 */
public interface OrisClubKeyPort {

    void store(String clubKey);

    boolean isSet();

    void clear();
}
