package com.klabis.events.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
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

    @RecordBuilder
    public record CreateDiscipline(
            @NotBlank(message = "Discipline code is required")
            String code,
            @NotBlank(message = "Discipline name is required")
            String name
    ) {}

    private Discipline(DisciplineId id, String code, String name, AuditMetadata auditMetadata) {
        this.id = id;
        this.code = code;
        this.name = name;
        updateAuditMetadata(auditMetadata);
    }

    public static Discipline create(CreateDiscipline command) {
        validateCode(command.code());
        validateName(command.name());
        return new Discipline(DisciplineId.generate(), command.code(), command.name(), null);
    }

    public static Discipline reconstruct(DisciplineId id, String code, String name, AuditMetadata auditMetadata) {
        return new Discipline(id, code, name, auditMetadata);
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
        return "Discipline{id=" + id + ", code='" + code + "', name='" + name + "'}";
    }
}
