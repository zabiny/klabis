package com.klabis.events.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.CategoryPresetId;
import com.klabis.events.domain.CategoryPreset;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.klabis.events.infrastructure.jdbc.CsvListConverter.deserialize;
import static com.klabis.events.infrastructure.jdbc.CsvListConverter.serialize;

@Table("category_presets")
class CategoryPresetMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @Column("categories")
    private String categories;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("modified_at")
    private Instant lastModifiedAt;

    @LastModifiedBy
    @Column("modified_by")
    private String lastModifiedBy;

    @Version
    @Column("version")
    private Long version;

    @Transient
    private boolean isNew = true;

    protected CategoryPresetMemento() {
    }

    static CategoryPresetMemento from(CategoryPreset preset) {
        Assert.notNull(preset, "CategoryPreset must not be null");

        CategoryPresetMemento memento = new CategoryPresetMemento();
        memento.id = preset.getId().value();
        memento.name = preset.getName();
        memento.categories = serialize(preset.getCategories());

        memento.createdAt = preset.getCreatedAt();
        memento.createdBy = preset.getCreatedBy();
        memento.lastModifiedAt = preset.getLastModifiedAt();
        memento.lastModifiedBy = preset.getLastModifiedBy();

        memento.isNew = (preset.getAuditMetadata() == null);

        return memento;
    }

    CategoryPreset toPreset() {
        List<String> categoriesList = deserialize(this.categories);

        return CategoryPreset.reconstruct(
                new CategoryPresetId(this.id),
                this.name,
                categoriesList,
                new AuditMetadata(this.createdAt, this.createdBy, this.lastModifiedAt, this.lastModifiedBy, this.version)
        );
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
