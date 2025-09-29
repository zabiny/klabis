package com.dpolach.inmemoryrepository;

import org.apache.commons.lang3.stream.Streams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

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
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        return Streams.of(entities).map(this::save).toList();
    }

    @Override
    public List<T> findAll() {
        return entityStore.findAll(domainClass);
    }

    @Override
    public List<T> findAllById(Iterable<ID> ids) {
        return Streams.of(ids).map(this::findById).flatMap(Optional::stream).toList();
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
    public boolean existsById(ID id) {
        return findById(id).isPresent();
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
    public void deleteAllById(Iterable<? extends ID> ids) {
        Streams.of(ids).forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        Streams.of(entities).forEach(this::delete);
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

    @NonNull
    @Override
    public Page<T> findAll(@NonNull Pageable pageable) {
        List<T> allData = findAll();
        return PageUtils.create(allData, pageable);
    }

    @Override
    public List<T> findAll(Predicate<T> predicate) {
        return findAll().stream().filter(predicate).toList();
    }

    @Override
    public Page<T> findAll(Predicate<T> predicate, Pageable pageable) {
        List<T> allItems = findAll(predicate);

        return PageUtils.create(allItems, pageable);
    }
}