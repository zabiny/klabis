package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.sync.application.SyncPairing;
import com.klabis.sync.application.SyncPairingLookup;
import com.klabis.sync.domain.SyncTarget;
import org.springframework.hateoas.RepresentationModel;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@MvcComponent
public class SyncLinks {

    private final SyncPairingLookup pairingLookup;

    SyncLinks(SyncPairingLookup pairingLookup) {
        this.pairingLookup = pairingLookup;
    }

    public void addTo(RepresentationModel<?> model, SyncTarget target) {
        if (pairingLookup.pairingOf(target) == SyncPairing.NONE) {
            return;
        }
        SyncEntityTypeParam entityType = SyncEntityTypeResolver.toParam(target.entityType());
        klabisLinkTo(methodOn(SyncApi.class).getSyncState(entityType, target.entityId()))
                .ifPresent(link -> model.add(link.withRel("sync")));
    }
}
