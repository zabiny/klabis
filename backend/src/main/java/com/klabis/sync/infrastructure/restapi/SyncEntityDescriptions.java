package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@MvcComponent
class SyncEntityDescriptions {

    private static final Logger log = LoggerFactory.getLogger(SyncEntityDescriptions.class);

    private final Map<SyncEntityType, SyncEntityDescriber> describers;

    // ObjectProvider rather than List: web slices without any describer must still start.
    SyncEntityDescriptions(ObjectProvider<SyncEntityDescriber> describers) {
        this.describers = describers.stream().collect(Collectors.toUnmodifiableMap(
                SyncEntityDescriber::entityType, Function.identity(), (first, second) -> {
                    throw new IllegalStateException("Multiple describers for " + first.entityType());
                }));
    }

    Optional<EntityDescription> describe(SyncTarget target) {
        return Optional.ofNullable(describers.get(target.entityType()))
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
