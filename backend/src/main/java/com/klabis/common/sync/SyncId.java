package com.klabis.common.sync;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

@ValueObject
// Use factory methods to create instances of this class (#localId and #externalId)
public record SyncId(SyncType type, SyncParty party, String idValue) implements Identifier {

    public SyncId {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (party == null) {
            throw new IllegalArgumentException("party must not be null");
        }
        if (idValue == null) {
            throw new IllegalArgumentException("idValue must not be null");
        }
    }

    public static SyncId localId(SyncType type, String idValue) {
        return new SyncId(type, SyncParty.LOCAL, idValue);
    }

    public static SyncId externalId(SyncType type, String idValue) {
        return new SyncId(type, SyncParty.EXTERNAL, idValue);
    }

    public boolean isLocalId() {
        return party == SyncParty.LOCAL;
    }

    public boolean isExternalId() {
        return party == SyncParty.EXTERNAL;
    }

}
