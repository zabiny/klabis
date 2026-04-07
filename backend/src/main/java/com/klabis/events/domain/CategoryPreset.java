package com.klabis.events.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.events.CategoryPresetId;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AggregateRoot
public class CategoryPreset extends KlabisAggregateRoot<CategoryPreset, CategoryPresetId> {

    @Identity
    private final CategoryPresetId id;
    private String name;
    private List<String> categories;

    // ========== Nested Command Records ==========

    @RecordBuilder
    public record CreateCategoryPreset(
            @NotBlank(message = "Preset name is required")
            @Size(max = 200, message = "Preset name must not exceed 200 characters")
            String name,
            List<String> categories
    ) {
        public static CreateCategoryPreset from(CategoryPreset preset) {
            return new CreateCategoryPreset(preset.name, preset.categories);
        }
    }

    @RecordBuilder
    public record UpdateCategoryPreset(
            @NotBlank(message = "Preset name is required")
            @Size(max = 200, message = "Preset name must not exceed 200 characters")
            String name,
            List<String> categories
    ) {
        public static UpdateCategoryPreset from(CategoryPreset preset) {
            return new UpdateCategoryPreset(preset.name, preset.categories);
        }
    }

    private CategoryPreset(CategoryPresetId id, String name, List<String> categories, AuditMetadata auditMetadata) {
        this.id = id;
        this.name = name;
        this.categories = categories != null ? new ArrayList<>(categories) : new ArrayList<>();
        updateAuditMetadata(auditMetadata);
    }

    public static CategoryPreset create(CreateCategoryPreset command) {
        validateName(command.name());
        return new CategoryPreset(CategoryPresetId.generate(), command.name(), command.categories(), null);
    }

    public static CategoryPreset reconstruct(CategoryPresetId id, String name, List<String> categories, AuditMetadata auditMetadata) {
        return new CategoryPreset(id, name, categories, auditMetadata);
    }

    public void update(UpdateCategoryPreset command) {
        validateName(command.name());
        this.name = command.name();
        this.categories = command.categories() != null ? new ArrayList<>(command.categories()) : new ArrayList<>();
    }

    private static void validateName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Preset name is required");
        }
    }

    @Override
    public CategoryPresetId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getCategories() {
        return Collections.unmodifiableList(categories);
    }

    @Override
    public String toString() {
        return "CategoryPreset{id=" + id + ", name='" + name + "'}";
    }
}
