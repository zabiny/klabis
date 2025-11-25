package com.dpolach.inmemoryrepository;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.EntityInformation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

class SimpleInMemoryRepository<T, ID> implements InMemoryRepository<T, ID> {

    private final Class<T> domainClass;
    private final EntityInformation<T, ID> entityInformation;
    private final InMemoryEntityStores entityStore;

    public SimpleInMemoryRepository(Class<T> domainClass, EntityInformation<T, ID> entityInformation,
                                    InMemoryEntityStores entityStore) {
        this.domainClass = domainClass;
        this.entityInformation = entityInformation;
        this.entityStore = entityStore;
    }

    @Override
    public List<T> findAll() {
        return entityStore.findAll(domainClass);
    }

    @Override
    public long count() {
        return entityStore.findAll(domainClass).size();
    }

    @Override
    public Optional<T> findById(ID id) {
        return entityStore.findAll(domainClass)
                .stream()
                .filter(entity -> Objects.equals(entityInformation.getId(entity), id))
                .findFirst();
    }

    @Override
    public <S extends T> S save(S entity) {
        return entityStore.save(entity);
    }

    @Override
    public void delete(T entity) {
        entityStore.delete(entity);
    }

    @Override
    public void deleteAll() {
        entityStore.findAll(entityInformation.getJavaType()).forEach(this::delete);
    }

    @Override
    public void deleteById(ID id) {
        entityStore.deleteById(domainClass, id);
    }

    @NonNull
    @Override
    public Iterable<T> findAll(@NonNull Sort sort) {
        return entityStore.findAll(domainClass).stream()
                .sorted(SortComparator.of(sort))
                .toList();
    }

}