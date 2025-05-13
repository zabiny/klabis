package com.dpolach.inmemoryrepository;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface InMemoryRepository<T, ID> extends Repository<T, ID> {

    List<T> findAll();

    Optional<T> findById(ID id);

    <S extends T> S save(S entity);

    void delete(T entity);

    void deleteById(ID id);

    List<T> findAll(Predicate<T> predicate);

    Optional<T> findOne(Predicate<T> predicate);
}
