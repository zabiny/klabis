package com.dpolach.inmemoryrepository;

import org.apache.commons.lang3.stream.Streams;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@NoRepositoryBean
public interface InMemoryRepository<T, ID> extends ListCrudRepository<T, ID>, PagingAndSortingRepository<T, ID> {

    @NonNull
    @Override
    default Page<T> findAll(@NonNull Pageable pageable) {
        List<T> allData = findAll();
        return PageUtils.create(allData, pageable);
    }

    default List<T> findAll(Predicate<T> predicate) {
        return findAll().stream().filter(predicate).toList();
    }

    default Page<T> findAll(Predicate<T> predicate, Pageable pageable) {
        List<T> allItems = findAll(predicate);

        return PageUtils.create(allItems, pageable);
    }


    @Override
    default <S extends T> List<S> saveAll(Iterable<S> entities) {
        return Streams.of(entities).map(this::save).toList();
    }

    @Override
    default List<T> findAllById(Iterable<ID> ids) {
        return Streams.of(ids).map(this::findById).flatMap(Optional::stream).toList();
    }

    @Override
    default boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    default void deleteAllById(Iterable<? extends ID> ids) {
        Streams.of(ids).forEach(this::deleteById);
    }

    @Override
    default void deleteAll(Iterable<? extends T> entities) {
        Streams.of(entities).forEach(this::delete);
    }


}
