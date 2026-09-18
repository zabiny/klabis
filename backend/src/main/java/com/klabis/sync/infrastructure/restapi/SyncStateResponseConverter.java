package com.klabis.sync.infrastructure.restapi;

import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncBaseline;
import com.klabis.sync.domain.SyncProjectionFieldReader;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncStatus;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.List;
import java.util.Map;

/**
 * Maps a {@link SyncRecord} to its response shape (design.md D14). Not a MapStruct
 * {@code @Mapper} — the mapping is conditional (diverged fields only in CONFLICT,
 * baselineExternal only while diverged) rather than a field-to-field copy, and the
 * projections need {@link SyncProjectionFieldReader} to turn into plain maps.
 * <p>
 * Every generated enum property (`entityType`, `status`, `externalSystem`,
 * `lastDirection`, `changedSides` values) is built via that enum's own
 * {@code fromValue(String)}, binding to the wire value the domain enum's constant
 * happens to share — never {@code valueOf(domainEnum.name())} on the generated side,
 * since the two enums' constant names are not guaranteed to match (e.g.
 * {@code SyncEntityTypeParam.EVENTS} vs. domain {@code SyncEntityType.EVENT}).
 * <p>
 * Deliberately <b>not</b> a Spring {@code Converter} bean, unlike the usual mapping
 * pattern in this codebase: a {@code Converter} is registered into the global
 * {@code mvcConversionService} and therefore constructed by every {@code @WebMvcTest}
 * slice in the whole app, but its dependencies here ({@link SyncProjectionFieldReader},
 * {@link SynchronizationPort}) are sync-module-specific and not available in an
 * unrelated slice — {@code MemberIdToUuidConverter}/{@code RegisterNewMemberConverter}
 * avoid this because they take no constructor dependencies at all. Constructed
 * directly by {@link SynchronizationController} instead, so it is invisible to every
 * other module's tests.
 */
class SyncStateResponseConverter {

    private final SyncProjectionFieldReader fieldReader;
    private final SynchronizationPort synchronizationPort;

    SyncStateResponseConverter(SyncProjectionFieldReader fieldReader, SynchronizationPort synchronizationPort) {
        this.fieldReader = fieldReader;
        this.synchronizationPort = synchronizationPort;
    }

    SyncStateResponse convert(SyncRecord record) {
        SyncBaseline baseline = record.getBaseline();
        boolean inConflict = record.getStatus() == SyncStatus.CONFLICT;

        Map<String, Object> local = record.getLocal() != null ? fieldReader.fields(record.getLocal().projection()) : null;
        Map<String, Object> external = record.getExternal() != null ? fieldReader.fields(record.getExternal().projection()) : null;
        Map<String, Object> baselineLocal = baseline != null ? fieldReader.fields(baseline.local().projection()) : null;
        Map<String, Object> baselineExternal = baseline != null && baseline.isDiverged()
                ? fieldReader.fields(baseline.external().projection())
                : null;

        Map<String, com.klabis.sync.domain.ChangedSide> changedSidesByField = inConflict
                ? record.changedSides(fieldReader)
                : Map.of();
        List<String> divergedFields = List.copyOf(changedSidesByField.keySet());
        Map<String, SyncStateResponseChangedSidesValue> changedSides = changedSidesByField.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> SyncStateResponseChangedSidesValue.fromValue(e.getValue().name())));

        SyncStateResponseLastDirection lastDirection = record.getLastDirection() != null
                ? SyncStateResponseLastDirection.fromValue(record.getLastDirection().name())
                : null;

        return SyncStateResponseBuilder.builder()
                .entityType(SyncEntityTypeParam.fromValue(record.getTarget().entityType().pathSegment()))
                .status(SyncStateResponseStatus.fromValue(record.getStatus().name()))
                .externalSystem(SyncStateResponseExternalSystem.fromValue(record.getExternalReference().system().name()))
                .externalId(record.getExternalReference().externalId())
                .lastSuccessfulSyncAt(JsonNullable.of(record.getLastSuccessfulSyncAt()))
                .lastDirection(JsonNullable.of(lastDirection))
                .nextAttemptDueAt(JsonNullable.of(record.getNextAttemptDueAt()))
                .failedAttemptsSinceLastSuccess(synchronizationPort.failedAttemptsSinceLastSuccess(record.getId()))
                .acceptedDivergence(baseline != null && baseline.isDiverged())
                .divergedFields(divergedFields)
                .changedSides(changedSides)
                .local(JsonNullable.of(local))
                .external(JsonNullable.of(external))
                .baseline(JsonNullable.of(baselineLocal))
                .baselineExternal(baselineExternal)
                .build();
    }
}
