package com.dpolach.inmemoryrepository;

import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.util.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class InMemoryEntityInformation<T, ID> implements EntityInformation<T, ID> {

    private final Class<T> domainClass;
    private final Field idField;

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryEntityInformation.class);

    static class NoIdFieldException extends RuntimeException {
        public NoIdFieldException(String message) {
            super(message);
        }
    }

    public static <T, ID> InMemoryEntityInformation<T, ID> createEntityInformation(RepositoryMetadata repositoryMetadata) {
        Class<?> domainClass = repositoryMetadata.getDomainType();
        //Assert.isTrue(domainClass.isAnnotationPresent(AggregateRoot.class), "Domain class used as repository entity must be annotated with %s but %s is not".formatted(AggregateRoot.class.getCanonicalName(), domainClass.getCanonicalName()));
        if (!AbstractAggregateRoot.class.isAssignableFrom(domainClass)) {
            LOG.warn("Creating InMemoryEntityInformation for domainClass {} which doesn't inherit AbstractAggregateRoot",
                    domainClass);
        }

        Field idField = findEntityIdField(domainClass).orElseThrow(() -> new NoIdFieldException(
                "Couldn't detect ID field for domain class %s from repository %s".formatted(domainClass.getCanonicalName(),
                        repositoryMetadata.getRepositoryInterface().getSimpleName())));

        return new InMemoryEntityInformation<>((Class<T>) domainClass, idField);
    }

    @SuppressWarnings("unchecked")
    private InMemoryEntityInformation(@NonNull Class<T> domainClass, @NonNull Field idField) {
        this.domainClass = domainClass;
        this.idField = idField;
        ReflectionUtils.makeAccessible(this.idField);
    }

    private static <T> Optional<Field> findEntityIdField(Class<T> domainClass) {
        // Hledáme pole s anotací @Id
        List<Field> idFields = new ArrayList<>();
        ReflectionUtils.doWithFields(domainClass, field -> {
            if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(Identity.class)) {
                idFields.add(field);
            }
        });

        if (idFields.isEmpty()) {
            // Pokud není nalezeno žádné pole s anotací @Id, hledáme pole s názvem "id"
            return Optional.ofNullable(ReflectionUtils.findField(domainClass, "id"));
        } else {
            return Optional.of(idFields.getFirst());
        }
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