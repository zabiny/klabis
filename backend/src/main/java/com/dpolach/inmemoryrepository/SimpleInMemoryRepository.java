package com.dpolach.inmemoryrepository;

import org.springframework.data.repository.core.EntityInformation;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SimpleInMemoryRepository<T, ID extends Serializable> implements InMemoryRepository<T, ID> {

    private final Class<T> domainClass;
    private final EntityInformation<T, ID> entityInformation;
    private final InMemoryEntityStore entityStore;

    public SimpleInMemoryRepository(Class<T> domainClass, EntityInformation<T, ID> entityInformation,
                                    InMemoryEntityStore entityStore) {
        this.domainClass = domainClass;
        this.entityInformation = entityInformation;
        this.entityStore = entityStore;
    }

    @Override
    public List<T> findAll() {
        return entityStore.findAll(domainClass);
    }

    @Override
    public Optional<T> findById(ID id) {
        return entityStore.findById(domainClass, id);
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
    public void deleteById(ID id) {
        entityStore.deleteById(domainClass, id);
    }

    @Override
    public List<T> findAll(Predicate<T> predicate) {
        return entityStore.findAll(domainClass, predicate);
    }

    @Override
    public Optional<T> findOne(Predicate<T> predicate) {
        return entityStore.findOne(domainClass, predicate);
    }
}