package com.klabis.events.domain;

import com.klabis.events.CategoryPresetId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;
import java.util.Optional;

@SecondaryPort
public interface CategoryPresetRepository {

    CategoryPreset save(CategoryPreset preset);

    Optional<CategoryPreset> findById(CategoryPresetId id);

    List<CategoryPreset> findAll();

    void deleteById(CategoryPresetId id);
}
