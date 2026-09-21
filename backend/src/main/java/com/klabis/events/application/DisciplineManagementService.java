package com.klabis.events.application;

import com.klabis.events.DisciplineId;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineNotArchivedException;
import com.klabis.events.domain.DisciplineNotEditableException;
import com.klabis.events.domain.DisciplineNotFoundException;
import com.klabis.events.domain.DisciplineRepository;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncTarget;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD + archive/restore application service for {@link Discipline}, mirroring {@code
 * EventTypeManagementService} (design.md D7 of {@code sync-oris-disciplines}).
 * <p>
 * {@link #restore} reactivates the sync pairing itself, via {@link SynchronizationPort}
 * calls inlined here, rather than calling {@code DisciplineSyncListener#reactivate}: that
 * method lives on a {@code @PrimaryAdapter} in {@code events.infrastructure.orissync},
 * and this application service must not depend on an infrastructure adapter class — only
 * on another module's primary port, exactly the same rule that already lets {@code
 * DisciplineSyncListener} itself depend on {@link SynchronizationPort} directly. Design.md
 * D8's own wording assigns this call sequence to "the controller", written before the
 * task breakdown moved CRUD orchestration into this service ahead of the REST layer
 * (task 9 vs. 9.2/9.3); keeping it here instead means the future {@code
 * DisciplineController} only needs to call {@code restore}, with no sync-specific logic
 * of its own — consistent with every other business rule in this service, not split
 * across layers.
 */
@Service
class DisciplineManagementService implements DisciplineManagementPort {

    private final DisciplineRepository disciplineRepository;
    private final SynchronizationPort synchronizationPort;

    DisciplineManagementService(DisciplineRepository disciplineRepository, SynchronizationPort synchronizationPort) {
        this.disciplineRepository = disciplineRepository;
        this.synchronizationPort = synchronizationPort;
    }

    @Transactional
    @Override
    public Discipline create(Discipline.CreateDiscipline command) {
        return disciplineRepository.save(Discipline.create(command));
    }

    @Transactional(readOnly = true)
    @Override
    public Discipline get(DisciplineId id) {
        return disciplineRepository.findById(id)
                .orElseThrow(() -> new DisciplineNotFoundException(id));
    }

    @Transactional
    @Override
    public Discipline update(DisciplineId id, String code, String name) {
        Discipline discipline = disciplineRepository.findById(id)
                .orElseThrow(() -> new DisciplineNotFoundException(id));

        if (synchronizationPort.findByTarget(targetFor(id)).isPresent()) {
            throw new DisciplineNotEditableException(id);
        }

        discipline.update(code, name);
        return disciplineRepository.save(discipline);
    }

    @Transactional
    @Override
    public void archive(DisciplineId id) {
        Discipline discipline = disciplineRepository.findById(id)
                .orElseThrow(() -> new DisciplineNotFoundException(id));

        // No EventType-reference guard here, unlike EventTypeManagementService.deleteEventType —
        // archiving a Discipline never breaks a reference (D8): the FK is untouched.
        discipline.archive();
        disciplineRepository.save(discipline);
    }

    @Transactional
    @Override
    public void restore(DisciplineId id, String actingUser) {
        Discipline discipline = disciplineRepository.findById(id)
                .orElseThrow(() -> new DisciplineNotFoundException(id));

        if (!discipline.isArchived()) {
            throw new DisciplineNotArchivedException(id);
        }

        discipline.restore();
        disciplineRepository.save(discipline);

        synchronizationPort.findByTarget(targetFor(id))
                .ifPresent(record -> synchronizationPort.pullAndEnroll(
                        SyncEntityType.DISCIPLINE, record.getExternalReference(), actingUser));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Discipline> list(Pageable pageable) {
        return disciplineRepository.findAll(pageable);
    }

    private static SyncTarget targetFor(DisciplineId id) {
        return new SyncTarget(SyncEntityType.DISCIPLINE, id.value().toString());
    }
}
