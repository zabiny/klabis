package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncResolution;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.util.List;
import java.util.Map;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisAffordWithValueOptions;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Renders the per-state affordances of {@code getSyncState} (design.md D14, tasks.md
 * 6.5): {@code synchronizeNow} while the record can still run an ordinary pass,
 * {@code acknowledgeSyncConflict}/{@code resolveSyncConflict} while a conflict stands —
 * split by whether it is already acknowledged and still current (design.md D7's
 * "resolution choice is only offered after confirming the difference") — and
 * {@code resetSyncRecord} once the record is terminally failed. Runs for every
 * response that can carry an acknowledged conflict, not only {@code getSyncState} —
 * {@code synchronizeNow}, {@code resolveSyncConflict} and {@code resetSyncRecord} can
 * all return a record that is, or has just become, an acknowledged conflict.
 */
@MvcComponent
class SyncStatePostprocessor extends ModelWithDomainPostprocessor<SyncStateResponse, SyncRecord> {

    private final SynchronizationPort synchronizationPort;

    SyncStatePostprocessor(SynchronizationPort synchronizationPort) {
        this.synchronizationPort = synchronizationPort;
    }

    @Override
    public void process(EntityModel<SyncStateResponse> dtoModel, SyncRecord record) {
        SyncEntityTypeParam entityType = SyncEntityTypeParam.fromValue(record.getTarget().entityType().pathSegment());
        String id = record.getTarget().entityId();

        klabisLinkTo(methodOn(SyncApi.class).getSyncState(entityType, id))
                .map(link -> {
                    var self = link.withSelfRel();
                    self = switch (record.getStatus()) {
                        case NEW, IN_SYNC, RETRYING -> self.andAffordances(
                                klabisAfford(methodOn(SyncApi.class).synchronizeNow(entityType, id, null)));
                        case CONFLICT -> withConflictAffordances(self, record, entityType, id);
                        case FAILED -> self.andAffordances(
                                klabisAfford(methodOn(SyncApi.class).resetSyncRecord(entityType, id, null)));
                        case RETIRED -> self;
                    };
                    return self;
                })
                .ifPresent(dtoModel::add);
    }

    private Link withConflictAffordances(
            Link self, SyncRecord record, SyncEntityTypeParam entityType, String id) {
        if (record.isAcknowledgementCurrent()) {
            // Only the directions the adapter can actually perform are offered (values only —
            // the frontend localises the labels, labels.enums.resolution). EnumSet.of/add
            // iterates in enum declaration order (INWARD, OUTWARD, ACCEPT_DIVERGENCE).
            List<String> resolutions = synchronizationPort.supportedResolutions(record).stream()
                    .map(SyncResolution::name)
                    .toList();
            return self.andAffordances(
                    klabisAffordWithValueOptions(
                            methodOn(SyncApi.class).resolveSyncConflict(entityType, id, null, null),
                            Map.of("resolution", resolutions)));
        }
        return self.andAffordances(
                klabisAfford(methodOn(SyncApi.class).acknowledgeSyncConflict(entityType, id, null)));
    }
}
