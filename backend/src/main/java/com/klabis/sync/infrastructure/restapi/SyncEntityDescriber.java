package com.klabis.sync.infrastructure.restapi;

import com.klabis.sync.domain.SyncEntityType;

import java.util.Optional;

/**
 * Presentation SPI: the module owning an entity kind describes its entities for the synchronisation overview.
 * One bean per {@link SyncEntityType}. Kept off {@code SynchronizationAdapter} because a resource link is web-layer
 * knowledge the domain port must not depend on.
 * <p>
 * Labels are intentionally looked up without the viewer's read permission: the overview is restricted to
 * {@code SYNC_MANAGE}, the audience that already sees both projections. The entity link stays authorization-checked.
 */
public interface SyncEntityDescriber {

    SyncEntityType entityType();

    /**
     * @return empty when the entity no longer exists
     */
    Optional<EntityDescription> describe(String entityId);
}
