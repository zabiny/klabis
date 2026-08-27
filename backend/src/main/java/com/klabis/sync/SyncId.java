package com.klabis.sync;

public record SyncId(SyncType type, SyncParty party, String idValue) {

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
