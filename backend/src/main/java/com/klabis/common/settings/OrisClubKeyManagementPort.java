package com.klabis.common.settings;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * Primary port for REST adapters that set, discard or check the club key. Like {@link OrisClubKeyPort}
 * it has no getter, so the key cannot be read back through it.
 */
@PrimaryPort
public interface OrisClubKeyManagementPort {

    void store(String clubKey);

    boolean isSet();

    void clear();
}
