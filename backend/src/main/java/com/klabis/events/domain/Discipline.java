package com.klabis.events.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.events.DisciplineArchivedEvent;
import com.klabis.events.DisciplineId;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

@AggregateRoot
public class Discipline extends KlabisAggregateRoot<Discipline, DisciplineId> {

    @Identity
    private final DisciplineId id;
    private String code;
    private String name;
    private boolean archived;

    @RecordBuilder
    public record CreateDiscipline(
            @NotBlank(message = "Discipline code is required")
            String code,
            @NotBlank(message = "Discipline name is required")
            String name
    ) {}

    private Discipline(DisciplineId id, String code, String name, boolean archived, AuditMetadata auditMetadata) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.archived = archived;
        updateAuditMetadata(auditMetadata);
    }

    public static Discipline create(CreateDiscipline command) {
        validateCode(command.code());
        validateName(command.name());
        return new Discipline(DisciplineId.generate(), command.code(), command.name(), false, null);
    }

    public static Discipline reconstruct(DisciplineId id, String code, String name, boolean archived, AuditMetadata auditMetadata) {
        return new Discipline(id, code, name, archived, auditMetadata);
    }

    private static void validateCode(String code) {
        Assert.hasText(code, "Discipline code is required");
    }

    private static void validateName(String name) {
        Assert.hasText(name, "Discipline name is required");
    }

    @Override
    public DisciplineId getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isArchived() {
        return archived;
    }

    /**
     * Soft-deletes the discipline (design.md D8 of {@code sync-oris-disciplines}).
     * Unlike {@code EventType.delete}, this never refuses regardless of how many
     * {@code EventType}s still reference this discipline's id — the FK is never
     * touched by archiving, so every existing reference stays valid.
     */
    public void archive() {
        this.archived = true;
        registerEvent(DisciplineArchivedEvent.fromAggregate(this));
    }

    /**
     * Reverses {@link #archive()}. Reactivating the sync pairing (if any) is the
     * sync engine's responsibility, not the aggregate's (D8) — this only flips the flag.
     */
    public void restore() {
        this.archived = false;
    }

    /**
     * Overwrites {@code code}/{@code name} from the paired ORIS record
     * ({@code DisciplineSyncAdapter.applyToLocal}, design.md D3). Both fields are
     * ORIS-owned once a discipline is paired, so this is the only way they change
     * after creation — there is no independent manual-edit path for a paired
     * discipline (D7).
     */
    public void update(String code, String name) {
        validateCode(code);
        validateName(name);
        this.code = code;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Discipline{id=" + id + ", code='" + code + "', name='" + name + "', archived=" + archived + "}";
    }
}
