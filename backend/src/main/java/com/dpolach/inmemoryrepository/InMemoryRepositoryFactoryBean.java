package com.dpolach.inmemoryrepository;

import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;

class InMemoryRepositoryFactoryBean extends TransactionalRepositoryFactoryBeanSupport<ListCrudRepository<Object, Object>, Object, Object> {
    /**
     * Creates a new {@link RepositoryFactoryBeanSupport} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    protected InMemoryRepositoryFactoryBean(Class<? extends ListCrudRepository<Object, Object>> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return new InMemoryRepositoryFactory();
    }
}
