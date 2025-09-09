package com.dpolach.inmemoryrepository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface InMemoryRepository<T, ID> extends ListCrudRepository<T, ID>, PagingAndSortingRepository<T, ID> {

}
