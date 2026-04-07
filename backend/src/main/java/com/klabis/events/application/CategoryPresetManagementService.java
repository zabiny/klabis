package com.klabis.events.application;

import com.klabis.events.CategoryPresetId;
import com.klabis.events.domain.CategoryPreset;
import com.klabis.events.domain.CategoryPresetRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class CategoryPresetManagementService implements CategoryPresetManagementPort {

    private final CategoryPresetRepository categoryPresetRepository;

    CategoryPresetManagementService(CategoryPresetRepository categoryPresetRepository) {
        this.categoryPresetRepository = categoryPresetRepository;
    }

    @Transactional
    @Override
    public CategoryPreset createPreset(CategoryPreset.CreateCategoryPreset command) {
        CategoryPreset preset = CategoryPreset.create(command);
        return categoryPresetRepository.save(preset);
    }

    @Transactional
    @Override
    public void updatePreset(CategoryPresetId id, CategoryPreset.UpdateCategoryPreset command) {
        CategoryPreset preset = categoryPresetRepository.findById(id)
                .orElseThrow(() -> new CategoryPresetNotFoundException(id));
        preset.update(command);
        categoryPresetRepository.save(preset);
    }

    @Transactional
    @Override
    public void deletePreset(CategoryPresetId id) {
        CategoryPreset preset = categoryPresetRepository.findById(id)
                .orElseThrow(() -> new CategoryPresetNotFoundException(id));
        categoryPresetRepository.deleteById(preset.getId());
    }

    @Transactional(readOnly = true)
    @Override
    public CategoryPreset getPreset(CategoryPresetId id) {
        return categoryPresetRepository.findById(id)
                .orElseThrow(() -> new CategoryPresetNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<CategoryPreset> listAll() {
        return categoryPresetRepository.findAll();
    }
}
