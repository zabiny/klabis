package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.users.ActingUser;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.User;
import com.klabis.sync.application.AttemptFilter;
import com.klabis.sync.application.AttemptView;
import com.klabis.sync.application.RecordView;
import com.klabis.sync.application.SyncHistoryQuery;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.application.SyncProjectionFieldsPort;
import com.klabis.sync.domain.SyncOutcome;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncResolution;
import com.klabis.sync.domain.SyncStatus;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

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
            SyncOutcomeParam outcome,
            Instant from,
            Instant to,
            Boolean includeUnchanged,
            @PageableDefault(size = 25) Pageable pageable) {
        AttemptFilter filter = new AttemptFilter(
                Boolean.TRUE.equals(includeUnchanged),
                entityType != null ? SyncEntityTypeResolver.resolve(entityType) : null,
                outcome != null ? SyncOutcome.valueOf(outcome.name()) : null,
                from,
                to);

        HalResponseContext.setContext(new SyncOverviewListing());
        return attemptsResponse(historyQuery.listAttempts(filter, pageable),
                view -> descriptions.describe(view.target()));
    }

    @Override
    public ResponseEntity<Page<SyncAttemptResponse>> listEntitySyncAttempts(
            @PathVariable SyncEntityTypeParam entityType,
            @PathVariable String id,
            Boolean includeUnchanged,
            @PageableDefault(size = 25) Pageable pageable) {
        SyncRecord record = requireRecord(entityType, id);
        Page<AttemptView> page = historyQuery.listAttemptsOf(
                record.getId(), Boolean.TRUE.equals(includeUnchanged), pageable);

        Optional<EntityDescription> description = descriptions.describe(record.getTarget());
        return attemptsResponse(page, view -> description);
    }

    @Override
    public ResponseEntity<List<SyncRecordSummaryResponse>> listSyncRecords(List<SyncStatusParam> status) {
        Set<SyncStatus> statuses = EnumSet.noneOf(SyncStatus.class);
        status.forEach(value -> statuses.add(SyncStatus.valueOf(value.name())));

        List<Described<RecordView>> described = historyQuery.listRecordsIn(statuses).stream()
                .map(view -> new Described<>(view, descriptions.describe(view.target())))
                .toList();

        HalResponseContext.setDomainList(described);
        return ResponseEntity.ok(described.stream().map(this::toRecordResponse).toList());
    }

    private ResponseEntity<Page<SyncAttemptResponse>> attemptsResponse(
            Page<AttemptView> page, Function<AttemptView, Optional<EntityDescription>> describer) {
        Page<Described<AttemptView>> described = page.map(view -> new Described<>(view, describer.apply(view)));

        HalResponseContext.setDomainList(described.getContent());
        return ResponseEntity.ok(described.map(this::toAttemptResponse));
    }

    private SyncRecordSummaryResponse toRecordResponse(Described<RecordView> described) {
        RecordView view = described.view();
        return SyncRecordSummaryResponseBuilder.builder()
                .entityType(SyncEntityTypeResolver.toParam(view.target().entityType()))
                .status(SyncStatusParam.valueOf(view.status().name()))
                .entityLabel(described.labelOrElse(view.externalId()))
                .externalId(view.externalId())
                .lastSuccessfulSyncAt(view.lastSuccessfulSyncAt())
                .failedAttemptsSinceLastSuccess(view.failedAttemptsSinceLastSuccess())
                .build();
    }

    private SyncAttemptResponse toAttemptResponse(Described<AttemptView> described) {
        AttemptView view = described.view();
        var attempt = view.attempt();
        return SyncAttemptResponseBuilder.builder()
                .startedAt(attempt.getStartedAt())
                .trigger(SyncAttemptResponseTrigger.fromValue(attempt.getTrigger().name()))
                .direction(attempt.getDirection() != null
                        ? SyncAttemptResponseDirection.fromValue(attempt.getDirection().name())
                        : null)
                .outcome(SyncOutcomeParam.valueOf(attempt.getOutcome().name()))
                .failureReason(attempt.getFailureReason())
                .performedBy(performedBy(attempt.getActingUser()))
                .entityType(SyncEntityTypeResolver.toParam(view.target().entityType()))
                .entityLabel(described.labelOrElse(view.externalId()))
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
