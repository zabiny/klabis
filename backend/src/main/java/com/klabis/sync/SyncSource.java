package com.klabis.sync;

import org.jmolecules.architecture.hexagonal.Port;

import java.util.Optional;

@Port
public interface SyncSource<T extends SyncData> {

    Optional<T> fetch(SyncId syncId);

    SyncId save(T data);

    SyncType type();

    SyncParty party();

    default boolean matches(SyncId syncId) {
        return syncId.type().equals(type()) && syncId.party().equals(party());
    }

    default boolean isOppositeOf(SyncSource<?> other) {
        return other.type().equals(type()) && party().isOppositeOf(other.party());
    };
}
