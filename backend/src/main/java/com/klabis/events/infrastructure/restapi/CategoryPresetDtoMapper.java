package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.CategoryPreset;

class CategoryPresetDtoMapper {

    static CategoryPresetDto toDto(CategoryPreset preset) {
        return new CategoryPresetDto(
                preset.getId(),
                preset.getName(),
                preset.getCategories()
        );
    }

    private CategoryPresetDtoMapper() {
    }
}
