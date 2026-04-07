package com.klabis.events.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.events.CategoryPresetId;

public class CategoryPresetNotFoundException extends ResourceNotFoundException {

    public CategoryPresetNotFoundException(CategoryPresetId id) {
        super("Category preset not found with ID: " + id);
    }
}
