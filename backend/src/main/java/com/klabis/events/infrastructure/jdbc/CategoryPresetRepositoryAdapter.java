package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.CategoryPresetId;
import com.klabis.events.domain.CategoryPreset;
import com.klabis.events.domain.CategoryPresetRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@SecondaryAdapter
@Repository
class CategoryPresetRepositoryAdapter implements CategoryPresetRepository {

    private final CategoryPresetJdbcRepository jdbcRepository;

    CategoryPresetRepositoryAdapter(CategoryPresetJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public CategoryPreset save(CategoryPreset preset) {
        return jdbcRepository.save(CategoryPresetMemento.from(preset)).toPreset();
    }

    @Override
    public Optional<CategoryPreset> findById(CategoryPresetId id) {
        return jdbcRepository.findById(id.value()).map(CategoryPresetMemento::toPreset);
    }

    @Override
    public List<CategoryPreset> findAll() {
        return StreamSupport.stream(jdbcRepository.findAll().spliterator(), false)
                .map(CategoryPresetMemento::toPreset)
                .toList();
    }

    @Override
    public void deleteById(CategoryPresetId id) {
        jdbcRepository.deleteById(id.value());
    }
}
