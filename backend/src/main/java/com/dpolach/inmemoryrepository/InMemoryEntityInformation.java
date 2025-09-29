package com.dpolach.inmemoryrepository;

import org.springframework.data.annotation.Id;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

class InMemoryEntityInformation<T, ID> implements EntityInformation<T, ID> {

    private final Class<T> domainClass;
    private final Field idField;

    @SuppressWarnings("unchecked")
    public InMemoryEntityInformation(Class<T> domainClass) {
        //Assert.isTrue(domainClass.isAnnotationPresent(AggregateRoot.class), "Domain class used as repository entity must be annotated with %s but %s is not".formatted(AggregateRoot.class.getCanonicalName(), domainClass.getCanonicalName()));
        this.domainClass = domainClass;

        // Hledáme pole s anotací @Id
        List<Field> idFields = new ArrayList<>();
        ReflectionUtils.doWithFields(domainClass, field -> {
            if (field.isAnnotationPresent(Id.class)) {
                idFields.add(field);
            }
        });

        if (idFields.isEmpty()) {
            // Pokud není nalezeno žádné pole s anotací @Id, hledáme pole s názvem "id"
            this.idField = ReflectionUtils.findField(domainClass, "id");
        } else {
            this.idField = idFields.get(0);
        }

        if (this.idField != null) {
            ReflectionUtils.makeAccessible(this.idField);
        }
        //Assert.notNull(this.idField, "Couldn't detect ID field for domain class %s".formatted(domainClass.getCanonicalName()));
    }

    @Override
    public boolean isNew(T entity) {
        if (idField == null) {
            return true;
        }

        ID id = getId(entity);
        return id == null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ID getId(T entity) {
        if (idField == null || entity == null) {
            return null;
        }

        return (ID) ReflectionUtils.getField(idField, entity);
    }

    @Override
    public Class<ID> getIdType() {
        @SuppressWarnings("unchecked")
        Class<ID> idType = idField != null ? (Class<ID>) idField.getType() : (Class<ID>) Serializable.class;
        return idType;
    }

    @Override
    public Class<T> getJavaType() {
        return domainClass;
    }

    public boolean isEntity(Class<?> entityClass) {
        return domainClass.isAssignableFrom(entityClass);
    }
}