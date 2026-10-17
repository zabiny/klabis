package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.sync.domain.SyncTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

@MvcComponent
class SyncEntityDescriptions {

    private static final Logger log = LoggerFactory.getLogger(SyncEntityDescriptions.class);

    private final ObjectProvider<SyncEntityDescriber> describers;

    SyncEntityDescriptions(ObjectProvider<SyncEntityDescriber> describers) {
        this.describers = describers;
    }

    Optional<EntityDescription> describe(SyncTarget target) {
        return describers.stream()
                .filter(describer -> describer.entityType() == target.entityType())
                .findFirst()
                .flatMap(describer -> describeSafely(describer, target));
    }

    // One faulty row (unparsable id, failing lookup) must not break the page; it falls back to the external id.
    private static Optional<EntityDescription> describeSafely(SyncEntityDescriber describer, SyncTarget target) {
        try {
            return describer.describe(target.entityId());
        } catch (RuntimeException e) {
            log.warn("Describing {} {} failed, falling back to external id", target.entityType(), target.entityId(), e);
            return Optional.empty();
        }
    }
}
