package com.klabis.events.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.CategoryPresetId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CategoryPreset domain tests")
class CategoryPresetTest {

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("should create preset with name and categories")
        void shouldCreatePresetWithNameAndCategories() {
            var command = new CategoryPreset.CreateCategoryPreset("Sprint Cup", List.of("M21", "W35", "D10"));

            CategoryPreset preset = CategoryPreset.create(command);

            assertThat(preset.getId()).isNotNull();
            assertThat(preset.getName()).isEqualTo("Sprint Cup");
            assertThat(preset.getCategories()).containsExactly("M21", "W35", "D10");
        }

        @Test
        @DisplayName("should create preset with empty categories when null provided")
        void shouldCreatePresetWithEmptyCategoriesWhenNull() {
            var command = new CategoryPreset.CreateCategoryPreset("Sprint Cup", null);

            CategoryPreset preset = CategoryPreset.create(command);

            assertThat(preset.getCategories()).isEmpty();
        }

        @Test
        @DisplayName("should throw when name is blank")
        void shouldThrowWhenNameIsBlank() {
            assertThatThrownBy(() -> CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("  ", List.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Preset name is required");
        }

        @Test
        @DisplayName("should throw when name is null")
        void shouldThrowWhenNameIsNull() {
            assertThatThrownBy(() -> CategoryPreset.create(new CategoryPreset.CreateCategoryPreset(null, List.of())))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should generate a unique ID")
        void shouldGenerateUniqueId() {
            var command = new CategoryPreset.CreateCategoryPreset("Forest Run", List.of("M21"));

            CategoryPreset first = CategoryPreset.create(command);
            CategoryPreset second = CategoryPreset.create(command);

            assertThat(first.getId()).isNotEqualTo(second.getId());
        }

        @Test
        @DisplayName("should start with null audit metadata (new aggregate)")
        void shouldStartWithNullAuditMetadata() {
            var command = new CategoryPreset.CreateCategoryPreset("Forest Run", List.of("M21"));
            CategoryPreset preset = CategoryPreset.create(command);

            assertThat(preset.getAuditMetadata()).isNull();
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("should update name and categories")
        void shouldUpdateNameAndCategories() {
            CategoryPreset preset = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Old Name", List.of("M21")));

            preset.update(new CategoryPreset.UpdateCategoryPreset("New Name", List.of("W35", "D10")));

            assertThat(preset.getName()).isEqualTo("New Name");
            assertThat(preset.getCategories()).containsExactly("W35", "D10");
        }

        @Test
        @DisplayName("should clear categories when null provided")
        void shouldClearCategoriesWhenNull() {
            CategoryPreset preset = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Cup", List.of("M21")));

            preset.update(new CategoryPreset.UpdateCategoryPreset("Cup", null));

            assertThat(preset.getCategories()).isEmpty();
        }

        @Test
        @DisplayName("should throw when updated name is blank")
        void shouldThrowWhenUpdatedNameIsBlank() {
            CategoryPreset preset = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Cup", List.of()));

            assertThatThrownBy(() -> preset.update(new CategoryPreset.UpdateCategoryPreset("", List.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Preset name is required");
        }
    }

    @Nested
    @DisplayName("reconstruct()")
    class ReconstructTests {

        @Test
        @DisplayName("should reconstruct preset with audit metadata")
        void shouldReconstructPresetWithAuditMetadata() {
            CategoryPresetId id = CategoryPresetId.generate();
            AuditMetadata audit = new AuditMetadata(Instant.now(), "admin", Instant.now(), "admin", 1L);

            CategoryPreset preset = CategoryPreset.reconstruct(id, "Forest Run", List.of("M21", "W35"), audit);

            assertThat(preset.getId()).isEqualTo(id);
            assertThat(preset.getName()).isEqualTo("Forest Run");
            assertThat(preset.getCategories()).containsExactly("M21", "W35");
            assertThat(preset.getAuditMetadata()).isEqualTo(audit);
        }
    }

    @Nested
    @DisplayName("getCategories()")
    class GetCategoriesTests {

        @Test
        @DisplayName("should return unmodifiable list")
        void shouldReturnUnmodifiableList() {
            CategoryPreset preset = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Cup", List.of("M21")));

            assertThatThrownBy(() -> preset.getCategories().add("W35"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
