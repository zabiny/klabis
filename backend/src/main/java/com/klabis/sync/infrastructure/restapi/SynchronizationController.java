package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.users.ActingUser;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.User;
import com.klabis.sync.application.AttemptFilter;
import com.klabis.sync.application.AttemptView;
import com.klabis.sync.application.SyncHistoryQuery;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.application.SyncProjectionFieldsPort;
import com.klabis.sync.domain.SyncOutcome;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncResolution;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

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
    private final SyncHistoryQuery historyQuery;
    private final UserService userService;
    private final SyncEntityDescriptions descriptions;

    // SyncStateResponseConverter is deliberately not a Spring bean of its own — see its
    // javadoc — so it is built here from the same two ports it would otherwise be
    // injected with.
    SynchronizationController(SynchronizationPort synchronizationPort, SyncProjectionFieldsPort fieldReader,
                              SyncHistoryQuery historyQuery, UserService userService,
                              SyncEntityDescriptions descriptions) {
        this.synchronizationPort = synchronizationPort;
        this.historyQuery = historyQuery;
        this.userService = userService;
        this.descriptions = descriptions;
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
                                                              @ActingUser UserId currentUser) {
        SyncRecord existing = requireRecord(entityType, id);
        SyncRecord record = synchronizationPort.synchronizeNow(existing.getId(), actingUser(currentUser));

        HalResponseContext.setDomain(record);
        return ResponseEntity.ok(responseConverter.convert(record));
    }

    @Override
    public ResponseEntity<SyncStateResponse> acknowledgeSyncConflict(@PathVariable SyncEntityTypeParam entityType, @PathVariable String id,
                                                                       @ActingUser UserId currentUser) {
        SyncRecord existing = requireRecord(entityType, id);
        SyncRecord record = synchronizationPort.acknowledgeConflict(existing.getId(), actingUser(currentUser));

        HalResponseContext.setDomain(record);
        return ResponseEntity.ok(responseConverter.convert(record));
    }

    @Override
    public ResponseEntity<SyncStateResponse> resolveSyncConflict(@PathVariable SyncEntityTypeParam entityType, @PathVariable String id,
                                                                   ResolveSyncConflictRequest request,
                                                                   @ActingUser UserId currentUser) {
        SyncRecord existing = requireRecord(entityType, id);
        SyncResolution resolution = SyncResolution.valueOf(request.resolution().getValue());
        SyncRecord record = synchronizationPort.resolveConflict(existing.getId(), resolution, actingUser(currentUser));

        HalResponseContext.setDomain(record);
        return ResponseEntity.ok(responseConverter.convert(record));
    }

    @Override
    public ResponseEntity<SyncStateResponse> resetSyncRecord(@PathVariable SyncEntityTypeParam entityType, @PathVariable String id,
                                                               @ActingUser UserId currentUser) {
        SyncRecord existing = requireRecord(entityType, id);
        SyncRecord record = synchronizationPort.reset(existing.getId(), actingUser(currentUser));

        HalResponseContext.setDomain(record);
        return ResponseEntity.ok(responseConverter.convert(record));
    }

    @Override
    public ResponseEntity<Page<SyncAttemptResponse>> listSyncAttempts(
            SyncEntityTypeParam entityType,
            String outcome,
            Instant from,
            Instant to,
            Boolean includeUnchanged,
            @PageableDefault(size = 25) Pageable pageable) {
        AttemptFilter filter = AttemptFilter.changedOnly()
                .withIncludeUnchanged(Boolean.TRUE.equals(includeUnchanged))
                .withEntityType(entityType != null ? SyncEntityTypeResolver.resolve(entityType) : null)
                .withOutcome(outcome != null ? SyncOutcome.valueOf(outcome) : null)
                .withFrom(from)
                .withTo(to);
        Page<AttemptView> page = historyQuery.listAttempts(filter, pageable);

        Page<DescribedAttempt> described = page.map(view -> new DescribedAttempt(view, descriptions.describe(view.target())));

        HalResponseContext.setDomainList(described.getContent());
        return ResponseEntity.ok(described.map(this::toResponse));
    }

    private SyncAttemptResponse toResponse(DescribedAttempt described) {
        AttemptView view = described.view();
        var attempt = view.attempt();
        return SyncAttemptResponseBuilder.builder()
                .startedAt(attempt.getStartedAt())
                .trigger(SyncAttemptResponseTrigger.fromValue(attempt.getTrigger().name()))
                .direction(attempt.getDirection() != null
                        ? SyncAttemptResponseDirection.fromValue(attempt.getDirection().name())
                        : null)
                .outcome(SyncAttemptResponseOutcome.fromValue(attempt.getOutcome().name()))
                .failureReason(attempt.getFailureReason())
                .performedBy(performedBy(attempt.getActingUser()))
                .entityType(SyncEntityTypeParam.fromValue(view.target().entityType().pathSegment()))
                .entityLabel(described.description().map(EntityDescription::label).orElse(view.externalId()))
                .externalId(view.externalId())
                .build();
    }

    private String performedBy(String actingUser) {
        if (actingUser == null) {
            return null;
        }
        UUID userUuid;
        try {
            userUuid = UUID.fromString(actingUser);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return userService.findUserById(new UserId(userUuid))
                .map(User::getUsername)
                .orElse(null);
    }

    private SyncRecord requireRecord(SyncEntityTypeParam entityTypeParam, String id) {
        SyncEntityType entityType = SyncEntityTypeResolver.resolve(entityTypeParam);
        SyncTarget target = new SyncTarget(entityType, id);
        return synchronizationPort.findByTarget(target)
                .orElseThrow(() -> new SyncRecordNotEnrolledException(target));
    }

    private static String actingUser(UserId currentUser) {
        return currentUser.uuid().toString();
    }
}
