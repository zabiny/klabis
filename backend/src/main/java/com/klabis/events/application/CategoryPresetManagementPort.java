package com.klabis.events.application;

import com.klabis.events.CategoryPresetId;
import com.klabis.events.domain.CategoryPreset;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

@PrimaryPort
public interface CategoryPresetManagementPort {

    CategoryPreset createPreset(CategoryPreset.CreateCategoryPreset command);

    void updatePreset(CategoryPresetId id, CategoryPreset.UpdateCategoryPreset command);

    void deletePreset(CategoryPresetId id);

    CategoryPreset getPreset(CategoryPresetId id);

    List<CategoryPreset> listAll();
}
