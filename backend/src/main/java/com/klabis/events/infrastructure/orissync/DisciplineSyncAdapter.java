package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.events.DisciplineId;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineNotFoundException;
import com.klabis.events.domain.DisciplineRepository;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.ExternalVersionToken;
import com.klabis.sync.domain.SyncCapabilities;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;
import com.klabis.sync.domain.SynchronizationAdapter;
import org.jmolecules.architecture.hexagonal.Application;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * The ORIS discipline {@link SynchronizationAdapter} (design.md D3): pull-only,
 * creating the local side but never writing outward — ORIS is the sole owner of a
 * discipline's {@code code}/{@code name} once paired, matching
 * {@code OrisEventSyncAdapter}'s shape exactly.
 * <p>
 * Reaches {@code events} through {@link DisciplineRepository} directly, both for the
 * local read and for the write side — unlike {@code OrisEventSyncAdapter}, there is
 * no {@code DisciplineManagementPort} yet (it is added later, task 7/D7), so this
 * class is a plain module-internal collaborator of {@code events.domain} rather than
 * a caller of any primary port.
 * <p>
 * Classified as {@link Application}, matching {@code OrisEventSyncAdapter}: this
 * class implements {@code sync}'s {@link SynchronizationAdapter} secondary port
 * while also driving {@link OrisApiClient}, the ORIS integration's own client — the
 * same "adapter into one module, driving collaborator of another" shape the
 * reference adapter already established (backend-patterns skill,
 * {@code synchronization-adapter.md}).
 */
@OrisIntegrationComponent
@Application
class DisciplineSyncAdapter implements SynchronizationAdapter {

    private static final SyncCapabilities CAPABILITIES = SyncCapabilities.pullOnlyCreating();

    private final DisciplineRepository disciplineRepository;
    private final OrisApiClient orisApiClient;

    DisciplineSyncAdapter(DisciplineRepository disciplineRepository, OrisApiClient orisApiClient) {
        this.disciplineRepository = disciplineRepository;
        this.orisApiClient = orisApiClient;
    }

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.DISCIPLINE;
    }

    @Override
    public ExternalSystem system() {
        return ExternalSystem.ORIS;
    }

    @Override
    public SyncCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public Class<? extends SyncProjection> projectionType() {
        return DisciplineProjection.class;
    }

    @Override
    public SyncProjection readLocal(String entityId) {
        Discipline discipline = findDiscipline(toDisciplineId(entityId));
        return DisciplineProjectionMapper.fromDiscipline(discipline);
    }

    @Override
    public SyncProjection readExternal(String externalId) {
        var entry = orisApiClient.listDisciplines().payload()
                .flatMap(disciplines -> disciplines.values().stream()
                        .filter(candidate -> externalId.equals(candidate.id()))
                        .findFirst())
                .orElseThrow(() -> new DisciplineNotFoundException(externalId));
        return DisciplineProjectionMapper.fromOrisEntry(entry);
    }

    /**
     * Always empty: {@code oris-client} offers no cheap per-discipline or per-list
     * version signal — {@code listDisciplines()} carries no version field, mirroring
     * {@code OrisEventSyncAdapter#externalVersion}. The engine falls back to a full
     * read on every pass, as it does for any adapter without a token (design.md D3).
     */
    @Override
    public Optional<ExternalVersionToken> externalVersion(String externalId) {
        return Optional.empty();
    }

    @Override
    @Transactional
    public void applyToLocal(String entityId, SyncProjection projection) {
        DisciplineId disciplineId = toDisciplineId(entityId);
        DisciplineProjection disciplineProjection = (DisciplineProjection) projection;

        Discipline discipline = findDiscipline(disciplineId);
        discipline.update(disciplineProjection.code(), disciplineProjection.name());

        disciplineRepository.save(discipline);
    }

    @Override
    @Transactional
    public String createLocal(SyncProjection projection) {
        DisciplineProjection disciplineProjection = (DisciplineProjection) projection;

        Discipline discipline = Discipline.create(
                new Discipline.CreateDiscipline(disciplineProjection.code(), disciplineProjection.name()));

        Discipline saved = disciplineRepository.save(discipline);
        return saved.getId().value().toString();
    }

    @Override
    public void applyToExternal(String externalId, SyncProjection projection) {
        throw new UnsupportedOperationException(
                "The ORIS discipline adapter declares no outward write capability");
    }

    private Discipline findDiscipline(DisciplineId disciplineId) {
        return disciplineRepository.findById(disciplineId)
                .orElseThrow(() -> new DisciplineNotFoundException(disciplineId));
    }

    private static DisciplineId toDisciplineId(String entityId) {
        return new DisciplineId(UUID.fromString(entityId));
    }
}
