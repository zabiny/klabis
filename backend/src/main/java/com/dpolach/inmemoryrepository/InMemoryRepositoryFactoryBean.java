package com.dpolach.inmemoryrepository;

import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

public class InMemoryRepositoryFactoryBean extends RepositoryFactoryBeanSupport<InMemoryRepository<Object, Object>, Object, Object> {
    /**
     * Creates a new {@link RepositoryFactoryBeanSupport} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    protected InMemoryRepositoryFactoryBean(Class<? extends InMemoryRepository<Object, Object>> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory() {
        return new InMemoryRepositoryFactory();
    }
}
