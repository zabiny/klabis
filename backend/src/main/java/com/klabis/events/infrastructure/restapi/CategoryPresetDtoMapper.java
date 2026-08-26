package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.CategoryPreset;

class CategoryPresetDtoMapper {

    static CategoryPresetDto toDto(CategoryPreset preset) {
        return CategoryPresetDtoBuilder.builder()
                .id(preset.getId().value())
                .name(preset.getName())
                .categories(preset.getCategories())
                .build();
    }

    private CategoryPresetDtoMapper() {
    }
}
