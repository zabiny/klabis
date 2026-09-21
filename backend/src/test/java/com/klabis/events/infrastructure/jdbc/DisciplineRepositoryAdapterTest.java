package com.klabis.events.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.events.DisciplineId;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineRepository;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Discipline JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = "DELETE FROM events.disciplines")
class DisciplineRepositoryAdapterTest {

    @Autowired
    private DisciplineRepository disciplineRepository;

    @Nested
    @DisplayName("save() and findById() round-trip")
    class SaveAndFindById {

        @Test
        @DisplayName("should persist and load discipline with code and name")
        void shouldPersistAndLoad() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));

            Discipline saved = disciplineRepository.save(discipline);
            Optional<Discipline> loaded = disciplineRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getCode()).isEqualTo("OB");
            assertThat(loaded.get().getName()).isEqualTo("Orientační běh");
        }

        @Test
        @DisplayName("should populate audit metadata after save")
        void shouldPopulateAuditMetadata() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("MTBO", "Orientační běh na kole"));

            Discipline saved = disciplineRepository.save(discipline);

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should return empty when not found")
        void shouldReturnEmptyWhenNotFound() {
            assertThat(disciplineRepository.findById(DisciplineId.generate())).isEmpty();
        }
    }
}
