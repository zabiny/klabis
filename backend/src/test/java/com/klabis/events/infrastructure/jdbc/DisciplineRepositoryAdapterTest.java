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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
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

        @Test
        @DisplayName("should persist and load archived flag")
        void shouldPersistAndLoadArchivedFlag() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("ARCH", "Archivovaná disciplína"));
            discipline.archive();

            Discipline saved = disciplineRepository.save(discipline);
            Optional<Discipline> loaded = disciplineRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().isArchived()).isTrue();
        }

        @Test
        @DisplayName("should default archived to false for newly created discipline")
        void shouldDefaultArchivedToFalse() {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("NEW", "Nová disciplína"));

            Discipline saved = disciplineRepository.save(discipline);
            Optional<Discipline> loaded = disciplineRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().isArchived()).isFalse();
        }
    }

    @Nested
    @DisplayName("findAllSorted()")
    class FindAllSorted {

        @Test
        @DisplayName("should return disciplines sorted by name, regardless of insertion order")
        void shouldReturnDisciplinesSortedByName() {
            disciplineRepository.save(Discipline.create(new Discipline.CreateDiscipline("SPR", "Sprint")));
            disciplineRepository.save(Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh")));
            disciplineRepository.save(Discipline.create(new Discipline.CreateDiscipline("MTBO", "Orientační běh na kole")));

            List<Discipline> disciplines = disciplineRepository.findAllSorted();

            assertThat(disciplines).extracting(Discipline::getName)
                    .containsExactly("Orientační běh", "Orientační běh na kole", "Sprint");
        }

        @Test
        @DisplayName("should return empty list when no disciplines exist")
        void shouldReturnEmptyListWhenNoDisciplines() {
            assertThat(disciplineRepository.findAllSorted()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll(Pageable) — paginated listing (D10)")
    class FindAllPaged {

        @Test
        @DisplayName("should return the requested page and correct total count")
        void shouldReturnRequestedPageAndTotal() {
            disciplineRepository.save(Discipline.create(new Discipline.CreateDiscipline("A1", "Alpha")));
            disciplineRepository.save(Discipline.create(new Discipline.CreateDiscipline("B1", "Bravo")));
            disciplineRepository.save(Discipline.create(new Discipline.CreateDiscipline("C1", "Charlie")));

            Page<Discipline> page = disciplineRepository.findAll(PageRequest.of(0, 2));

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getTotalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("should include archived disciplines in the listing")
        void shouldIncludeArchivedDisciplines() {
            Discipline archived = Discipline.create(new Discipline.CreateDiscipline("ARCH2", "Archived one"));
            archived.archive();
            disciplineRepository.save(archived);

            Page<Discipline> page = disciplineRepository.findAll(PageRequest.of(0, 10));

            assertThat(page.getContent()).extracting(Discipline::isArchived).containsExactly(true);
        }

        @Test
        @DisplayName("should return an empty page when no disciplines exist")
        void shouldReturnEmptyPageWhenNoDisciplines() {
            Page<Discipline> page = disciplineRepository.findAll(PageRequest.of(0, 10));

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }
    }
}
