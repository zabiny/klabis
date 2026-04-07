package com.klabis.events.application;

import com.klabis.events.CategoryPresetId;
import com.klabis.events.domain.CategoryPreset;
import com.klabis.events.domain.CategoryPresetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CategoryPresetManagementService tests")
@ExtendWith(MockitoExtension.class)
class CategoryPresetManagementServiceTest {

    @Mock
    private CategoryPresetRepository categoryPresetRepository;

    @InjectMocks
    private CategoryPresetManagementService service;

    @Nested
    @DisplayName("createPreset()")
    class CreatePresetTests {

        @Test
        @DisplayName("should create and save preset")
        void shouldCreateAndSavePreset() {
            var command = new CategoryPreset.CreateCategoryPreset("Sprint Cup", List.of("M21", "W35"));
            CategoryPreset saved = CategoryPreset.create(command);
            when(categoryPresetRepository.save(any())).thenReturn(saved);

            CategoryPreset result = service.createPreset(command);

            assertThat(result).isEqualTo(saved);
            verify(categoryPresetRepository).save(any(CategoryPreset.class));
        }
    }

    @Nested
    @DisplayName("updatePreset()")
    class UpdatePresetTests {

        @Test
        @DisplayName("should update existing preset")
        void shouldUpdateExistingPreset() {
            CategoryPresetId id = CategoryPresetId.generate();
            CategoryPreset existing = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Old", List.of("M21")));
            var command = new CategoryPreset.UpdateCategoryPreset("New", List.of("W35"));
            when(categoryPresetRepository.findById(id)).thenReturn(Optional.of(existing));
            when(categoryPresetRepository.save(any())).thenReturn(existing);

            service.updatePreset(id, command);

            verify(categoryPresetRepository).save(existing);
        }

        @Test
        @DisplayName("should throw CategoryPresetNotFoundException when preset not found")
        void shouldThrowWhenPresetNotFound() {
            CategoryPresetId id = CategoryPresetId.generate();
            when(categoryPresetRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePreset(id, new CategoryPreset.UpdateCategoryPreset("X", List.of())))
                    .isInstanceOf(CategoryPresetNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deletePreset()")
    class DeletePresetTests {

        @Test
        @DisplayName("should delete existing preset")
        void shouldDeleteExistingPreset() {
            CategoryPresetId id = CategoryPresetId.generate();
            CategoryPreset existing = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Cup", List.of()));
            when(categoryPresetRepository.findById(id)).thenReturn(Optional.of(existing));

            service.deletePreset(id);

            verify(categoryPresetRepository).deleteById(id);
        }

        @Test
        @DisplayName("should throw CategoryPresetNotFoundException when preset not found")
        void shouldThrowWhenPresetNotFound() {
            CategoryPresetId id = CategoryPresetId.generate();
            when(categoryPresetRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletePreset(id))
                    .isInstanceOf(CategoryPresetNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getPreset()")
    class GetPresetTests {

        @Test
        @DisplayName("should return preset when found")
        void shouldReturnPresetWhenFound() {
            CategoryPresetId id = CategoryPresetId.generate();
            CategoryPreset preset = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Cup", List.of("M21")));
            when(categoryPresetRepository.findById(id)).thenReturn(Optional.of(preset));

            CategoryPreset result = service.getPreset(id);

            assertThat(result).isEqualTo(preset);
        }

        @Test
        @DisplayName("should throw CategoryPresetNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            CategoryPresetId id = CategoryPresetId.generate();
            when(categoryPresetRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPreset(id))
                    .isInstanceOf(CategoryPresetNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listAll()")
    class ListAllTests {

        @Test
        @DisplayName("should return all presets from repository")
        void shouldReturnAllPresets() {
            List<CategoryPreset> presets = List.of(
                    CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("A", List.of())),
                    CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("B", List.of()))
            );
            when(categoryPresetRepository.findAll()).thenReturn(presets);

            List<CategoryPreset> result = service.listAll();

            assertThat(result).hasSize(2);
        }
    }
}
