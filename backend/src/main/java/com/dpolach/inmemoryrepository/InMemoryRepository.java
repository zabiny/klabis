package com.dpolach.inmemoryrepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.function.Predicate;

public interface InMemoryRepository<T, ID> extends ListCrudRepository<T, ID>, PagingAndSortingRepository<T, ID> {

    public List<T> findAll(Predicate<T> predicate);

    public Page<T> findAll(Predicate<T> predicate, Pageable pageable);

}
