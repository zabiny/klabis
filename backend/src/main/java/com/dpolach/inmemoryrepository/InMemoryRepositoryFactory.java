package com.dpolach.inmemoryrepository;

import com.dpolach.inmemoryrepository.query.InMemoryQueryLookupStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

import java.util.Optional;

class InMemoryRepositoryFactory extends RepositoryFactorySupport {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRepositoryFactory.class);
    private ObjectProvider<InMemoryEntityStore> entityStoreProvider;

    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return new InMemoryEntityInformation<>(domainClass);
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        Class<Object> domainClass = (Class<Object>) information.getDomainType();
        EntityInformation<Object, ?> entityInformation = getEntityInformation(domainClass);

        log.debug("Creating repository for {} with entity information {}",
                information.getDomainType(),
                entityInformation.getIdType());

        InMemoryEntityStore entityStore = getEntityStore().orElseThrow();

        @SuppressWarnings("unchecked")
        InMemoryRepository<Object, Object> repository =
                new SimpleInMemoryRepository<>((Class<Object>) information.getDomainType(),
                        (EntityInformation<Object, Object>) entityInformation,
                        entityStore);

        // Registrujeme entityInformation, abychom ji mohli později použít
        entityStore.register(domainClass, entityInformation);

        return repository;
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleInMemoryRepository.class;
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
                                                                   QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return getEntityStore().map(entityStore -> InMemoryQueryLookupStrategy.create(entityStore, key));
    }

    private Optional<InMemoryEntityStore> getEntityStore() {
        return Optional.ofNullable(entityStoreProvider.getIfAvailable());
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        super.setBeanFactory(beanFactory);
        this.entityStoreProvider = beanFactory.getBeanProvider(InMemoryEntityStore.class);
    }
}