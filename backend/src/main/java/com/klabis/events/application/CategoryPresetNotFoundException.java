package com.klabis.events.application;

import com.klabis.common.exceptions.ResourceNotFoundException;
import com.klabis.common.exceptions.UserFacingError;
import com.klabis.events.CategoryPresetId;

public class CategoryPresetNotFoundException extends ResourceNotFoundException implements UserFacingError {

    public CategoryPresetNotFoundException(CategoryPresetId id) {
        super("Category preset not found with ID: " + id);
    }
}
