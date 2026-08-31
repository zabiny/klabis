package com.klabis.sync.domain;

/**
 * The canonical field set of one side (local or external) of a synchronised entity,
 * in a shape shared by both sides (design.md D3).
 * <p>
 * The engine handles projections only through this interface: serialise, deserialise,
 * hash, compare field by field. It never knows the concrete shape. A concrete
 * implementation (one per {@link SyncEntityType}) is a plain data carrier with no
 * behaviour — typically a record — that serialises to and from JSON directly, with no
 * bespoke mapping layer.
 * <p>
 * Fields a Klabis module owns exclusively are simply absent from the projection: they
 * are invisible to synchronisation by construction, not by special-casing.
 */
public interface SyncProjection {

    SyncEntityType entityType();
}
