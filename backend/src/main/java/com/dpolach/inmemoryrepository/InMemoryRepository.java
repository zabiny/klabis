package com.dpolach.inmemoryrepository;

import org.springframework.data.repository.ListCrudRepository;

public interface InMemoryRepository<T, ID> extends ListCrudRepository<T, ID> {

}
