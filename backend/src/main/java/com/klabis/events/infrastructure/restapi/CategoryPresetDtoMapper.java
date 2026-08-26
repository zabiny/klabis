package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.CategoryPreset;

class CategoryPresetDtoMapper {

    static CategoryPresetDto toDto(CategoryPreset preset) {
        return new CategoryPresetDto(
                preset.getCategories(),
                preset.getId().value(),
                preset.getName()
        );
    }

    private CategoryPresetDtoMapper() {
    }
}
