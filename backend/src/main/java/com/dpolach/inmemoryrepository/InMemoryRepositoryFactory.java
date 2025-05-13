package com.dpolach.inmemoryrepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

import java.io.Serializable;
import java.util.Optional;

public class InMemoryRepositoryFactory extends RepositoryFactorySupport {

    private static final Logger log = LoggerFactory.getLogger(InMemoryRepositoryFactory.class);
    private final InMemoryEntityStore entityStore;

    public InMemoryRepositoryFactory() {
        super();
        this.entityStore = new InMemoryEntityStore();
    }

    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return new InMemoryEntityInformation<>(domainClass);
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        Class<Object> domainClass = (Class<Object>) information.getDomainType();
        EntityInformation<Object, ?> entityInformation = getEntityInformation(domainClass);

        log.debug("Creating repository for {} with entity information {}", information.getDomainType(), entityInformation.getIdType());

        @SuppressWarnings("unchecked")
        InMemoryRepository<Object, Serializable> repository =
                new SimpleInMemoryRepository<>((Class<Object>) information.getDomainType(),
                        (EntityInformation<Object, Serializable>) entityInformation,
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
        return Optional.of(InMemoryQueryLookupStrategy.create(entityStore, key));
    }
}