package com.klabis.events.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.events.CategoryPresetId;
import com.klabis.events.domain.CategoryPreset;
import com.klabis.events.domain.CategoryPresetRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CategoryPreset JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
class CategoryPresetJdbcRepositoryTest {

    @Autowired
    private CategoryPresetRepository categoryPresetRepository;

    @Nested
    @DisplayName("save() and findById() round-trip")
    class SaveAndFindById {

        @Test
        @DisplayName("should persist and load preset with all fields")
        void shouldPersistAndLoadPreset() {
            CategoryPreset preset = CategoryPreset.create(
                    new CategoryPreset.CreateCategoryPreset("Sprint Cup", List.of("M21", "W35", "D10")));

            CategoryPreset saved = categoryPresetRepository.save(preset);
            Optional<CategoryPreset> loaded = categoryPresetRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getName()).isEqualTo("Sprint Cup");
            assertThat(loaded.get().getCategories()).containsExactlyInAnyOrder("M21", "W35", "D10");
        }

        @Test
        @DisplayName("should persist preset with empty categories")
        void shouldPersistPresetWithEmptyCategories() {
            CategoryPreset preset = CategoryPreset.create(
                    new CategoryPreset.CreateCategoryPreset("Empty Preset", List.of()));

            CategoryPreset saved = categoryPresetRepository.save(preset);
            Optional<CategoryPreset> loaded = categoryPresetRepository.findById(saved.getId());

            assertThat(loaded).isPresent();
            assertThat(loaded.get().getCategories()).isEmpty();
        }

        @Test
        @DisplayName("should return empty when preset not found")
        void shouldReturnEmptyWhenNotFound() {
            Optional<CategoryPreset> result = categoryPresetRepository.findById(CategoryPresetId.generate());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should populate audit metadata after save")
        void shouldPopulateAuditMetadataAfterSave() {
            CategoryPreset preset = CategoryPreset.create(
                    new CategoryPreset.CreateCategoryPreset("Audit Test", List.of()));

            CategoryPreset saved = categoryPresetRepository.save(preset);

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("update via save()")
    class UpdateTests {

        @Test
        @DisplayName("should update preset name and categories")
        void shouldUpdatePreset() {
            CategoryPreset preset = categoryPresetRepository.save(
                    CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Old Name", List.of("M21"))));

            preset.update(new CategoryPreset.UpdateCategoryPreset("New Name", List.of("W35", "H55")));
            categoryPresetRepository.save(preset);

            Optional<CategoryPreset> loaded = categoryPresetRepository.findById(preset.getId());
            assertThat(loaded).isPresent();
            assertThat(loaded.get().getName()).isEqualTo("New Name");
            assertThat(loaded.get().getCategories()).containsExactlyInAnyOrder("W35", "H55");
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("should return all saved presets")
        void shouldReturnAllSavedPresets() {
            categoryPresetRepository.save(CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Preset A", List.of("M21"))));
            categoryPresetRepository.save(CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Preset B", List.of("W35"))));

            List<CategoryPreset> all = categoryPresetRepository.findAll();

            assertThat(all).hasSize(2);
            assertThat(all).extracting(CategoryPreset::getName).containsExactlyInAnyOrder("Preset A", "Preset B");
        }

        @Test
        @DisplayName("should return empty list when no presets exist")
        void shouldReturnEmptyListWhenNone() {
            List<CategoryPreset> all = categoryPresetRepository.findAll();

            assertThat(all).isEmpty();
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteTests {

        @Test
        @DisplayName("should delete preset by ID")
        void shouldDeletePreset() {
            CategoryPreset preset = categoryPresetRepository.save(
                    CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("To Delete", List.of())));

            categoryPresetRepository.deleteById(preset.getId());

            assertThat(categoryPresetRepository.findById(preset.getId())).isEmpty();
        }
    }
}
