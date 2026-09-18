package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.ui.HalResponseContext;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjectionFieldReader;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncResolution;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * One controller for every synchronisable entity type (design.md D14) — the
 * {@code {entityType}} path segment is translated to {@link SyncEntityType} by
 * {@link SyncEntityTypeResolver}, and the Klabis entity id together with it resolves a
 * {@link SyncRecord} via {@link SynchronizationPort#findByTarget}.
 */
@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
public class SynchronizationController implements SyncApi {

    private final SynchronizationPort synchronizationPort;
    private final SyncStateResponseConverter responseConverter;

    // SyncStateResponseConverter is deliberately not a Spring bean of its own — see its
    // javadoc — so it is built here from the same two ports it would otherwise be
    // injected with.
    SynchronizationController(SynchronizationPort synchronizationPort, SyncProjectionFieldReader fieldReader) {
        this.synchronizationPort = synchronizationPort;
        this.responseConverter = new SyncStateResponseConverter(fieldReader, synchronizationPort);
    }

    @Override
    public ResponseEntity<SyncStateResponse> getSyncState(@PathVariable SyncEntityTypeParam entityType, @PathVariable String id) {
        SyncRecord record = requireRecord(entityType, id);

        HalResponseContext.setDomain(record);
        return ResponseEntity.ok(responseConverter.convert(record));
    }

    @Override
    public ResponseEntity<SyncStateResponse> synchronizeNow(@PathVariable SyncEntityTypeParam entityType, @PathVariable String id,
                                                              @ActingUser CurrentUserData currentUser) {
        SyncRecord existing = requireRecord(entityType, id);
        SyncRecord record = synchronizationPort.synchronizeNow(existing.getId(), actingUser(currentUser));

        HalResponseContext.setDomain(record);
        return ResponseEntity.ok(responseConverter.convert(record));
    }

    @Override
    public ResponseEntity<SyncStateResponse> acknowledgeSyncConflict(@PathVariable SyncEntityTypeParam entityType, @PathVariable String id,
                                                                       @ActingUser CurrentUserData currentUser) {
        SyncRecord existing = requireRecord(entityType, id);
        SyncRecord record = synchronizationPort.acknowledgeConflict(existing.getId(), actingUser(currentUser));

        HalResponseContext.setDomain(record);
        return ResponseEntity.ok(responseConverter.convert(record));
    }

    @Override
    public ResponseEntity<SyncStateResponse> resolveSyncConflict(@PathVariable SyncEntityTypeParam entityType, @PathVariable String id,
                                                                   ResolveSyncConflictRequest request,
                                                                   @ActingUser CurrentUserData currentUser) {
        SyncRecord existing = requireRecord(entityType, id);
        SyncResolution resolution = SyncResolution.valueOf(request.resolution().getValue());
        SyncRecord record = synchronizationPort.resolveConflict(existing.getId(), resolution, actingUser(currentUser));

        HalResponseContext.setDomain(record);
        return ResponseEntity.ok(responseConverter.convert(record));
    }

    @Override
    public ResponseEntity<SyncStateResponse> resetSyncRecord(@PathVariable SyncEntityTypeParam entityType, @PathVariable String id,
                                                               @ActingUser CurrentUserData currentUser) {
        SyncRecord existing = requireRecord(entityType, id);
        SyncRecord record = synchronizationPort.reset(existing.getId(), actingUser(currentUser));

        HalResponseContext.setDomain(record);
        return ResponseEntity.ok(responseConverter.convert(record));
    }

    private SyncRecord requireRecord(SyncEntityTypeParam entityTypeParam, String id) {
        SyncEntityType entityType = SyncEntityTypeResolver.resolve(entityTypeParam);
        SyncTarget target = new SyncTarget(entityType, id);
        return synchronizationPort.findByTarget(target)
                .orElseThrow(() -> new SyncRecordNotEnrolledException(target));
    }

    private static String actingUser(CurrentUserData currentUser) {
        return currentUser.userId().uuid().toString();
    }
}
