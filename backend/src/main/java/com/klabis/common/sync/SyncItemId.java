package com.klabis.common.sync;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;
import org.jspecify.annotations.NonNull;


/**
 * Represents ID of synchronized record from either side of synchronization line. Typically initial data pull is triggered by "external" ID. After object is synchronized into local data storage, further operations are triggered by local ID of that object. The point is to keep "external IDs" only in data sync engine, so domain shouldn't need to keep remote ID or know it to trigger actual synchronization.
 *
 * @param party
 * @param type
 * @param idValue
 */
@ValueObject
// Use factory methods to create instances of this class (#localId and #externalId)
public record SyncItemId(SyncParty party, SyncType type, String idValue) implements Identifier {

    public SyncItemId {
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

    public static SyncItemId localId(SyncType type, String idValue) {
        return new SyncItemId(SyncParty.LOCAL, type, idValue);
    }

    public static SyncItemId externalId(SyncType type, String idValue) {
        return new SyncItemId(SyncParty.EXTERNAL, type, idValue);
    }

    public boolean isLocalId() {
        return party == SyncParty.LOCAL;
    }

    public boolean isExternalId() {
        return party == SyncParty.EXTERNAL;
    }

    @Override
    public @NonNull String toString() {
        return "%s-%s-%s".formatted(party.name(), type.name(), idValue);
    }
}
