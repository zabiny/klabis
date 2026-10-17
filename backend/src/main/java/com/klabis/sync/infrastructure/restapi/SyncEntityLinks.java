package com.klabis.sync.infrastructure.restapi;

import com.klabis.sync.domain.SyncTarget;
import org.springframework.hateoas.EntityModel;

import java.util.Optional;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

final class SyncEntityLinks {

    private SyncEntityLinks() {
    }

    static void addTo(EntityModel<?> model, SyncTarget target, Optional<EntityDescription> description) {
        SyncEntityTypeParam entityType = SyncEntityTypeParam.fromValue(target.entityType().pathSegment());

        klabisLinkTo(methodOn(SyncApi.class).getSyncState(entityType, target.entityId()))
                .ifPresent(link -> model.add(link.withRel("syncState")));

        description
                .map(EntityDescription::entityLink)
                .ifPresent(link -> model.add(link.withRel("entity")));
    }
}
