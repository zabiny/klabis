package com.klabis.events.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.DisciplineId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Discipline domain tests")
class DisciplineTest {

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should create discipline with code and name")
        void shouldCreateWithCodeAndName() {
            var command = new Discipline.CreateDiscipline("OB", "Orientační běh");

            Discipline discipline = Discipline.create(command);

            assertThat(discipline.getId()).isNotNull();
            assertThat(discipline.getCode()).isEqualTo("OB");
            assertThat(discipline.getName()).isEqualTo("Orientační běh");
        }

        @Test
        @DisplayName("should generate unique ids")
        void shouldGenerateUniqueIds() {
            var command = new Discipline.CreateDiscipline("OB", "Orientační běh");
            assertThat(Discipline.create(command).getId()).isNotEqualTo(Discipline.create(command).getId());
        }

        @Test
        @DisplayName("should start with null audit metadata")
        void shouldStartWithNullAuditMetadata() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            assertThat(discipline.getAuditMetadata()).isNull();
        }

        @Test
        @DisplayName("should throw when code is blank")
        void shouldThrowWhenCodeIsBlank() {
            assertThatThrownBy(() -> Discipline.create(new Discipline.CreateDiscipline("  ", "Orientační běh")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when code is null")
        void shouldThrowWhenCodeIsNull() {
            assertThatThrownBy(() -> Discipline.create(new Discipline.CreateDiscipline(null, "Orientační běh")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when name is blank")
        void shouldThrowWhenNameIsBlank() {
            assertThatThrownBy(() -> Discipline.create(new Discipline.CreateDiscipline("OB", "  ")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should throw when name is null")
        void shouldThrowWhenNameIsNull() {
            assertThatThrownBy(() -> Discipline.create(new Discipline.CreateDiscipline("OB", null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class ReconstructTests {

        @Test
        @DisplayName("should reconstruct with all fields including audit")
        void shouldReconstructWithAudit() {
            DisciplineId id = DisciplineId.generate();
            AuditMetadata audit = new AuditMetadata(Instant.now(), "admin", Instant.now(), "admin", 2L);

            Discipline discipline = Discipline.reconstruct(id, "OB", "Orientační běh", audit);

            assertThat(discipline.getId()).isEqualTo(id);
            assertThat(discipline.getCode()).isEqualTo("OB");
            assertThat(discipline.getName()).isEqualTo("Orientační běh");
            assertThat(discipline.getAuditMetadata()).isEqualTo(audit);
        }
    }
}
