package com.dpolach.inmemoryrepository.query;

import com.dpolach.inmemoryrepository.InMemoryEntityStores;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

public class InMemoryQueryLookupStrategy implements QueryLookupStrategy {

    private final InMemoryEntityStores entityStore;

    public InMemoryQueryLookupStrategy(InMemoryEntityStores entityStore) {
        Assert.notNull(entityStore, "EntityStore must not be null");
        this.entityStore = entityStore;
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
                                        ProjectionFactory factory, NamedQueries namedQueries) {

        InMemoryQueryMethod queryMethod = new InMemoryQueryMethod(method, metadata, factory);

        // Kontrola, zda existuje pojmenovaný dotaz
        String namedQueryName = queryMethod.getNamedQueryName();
        if (namedQueries.hasQuery(namedQueryName)) {
            return new StringBasedInMemoryQuery(namedQueries.getQuery(namedQueryName),
                    queryMethod, entityStore);
        }

        // Jinak vytvoříme dotaz na základě názvu metody
        return new PartTreeInMemoryQuery(queryMethod, entityStore);
    }

    /**
     * Vytvoří příslušnou strategii podle zadaného klíče.
     */
    public static QueryLookupStrategy create(InMemoryEntityStores entityStore, Key key) {
        Assert.notNull(entityStore, "EntityStore must not be null");

        if (key == null || key == Key.CREATE_IF_NOT_FOUND) {
            return new InMemoryQueryLookupStrategy(entityStore);
        }

        if (key == Key.USE_DECLARED_QUERY) {
            return new DeclaredQueryLookupStrategy(entityStore);
        }

        if (key == Key.CREATE) {
            return new CreateQueryLookupStrategy(entityStore);
        }

        throw new IllegalArgumentException("Unsupported key: " + key);
    }

    private static class DeclaredQueryLookupStrategy implements QueryLookupStrategy {

        private final InMemoryEntityStores entityStore;

        public DeclaredQueryLookupStrategy(InMemoryEntityStores entityStore) {
            this.entityStore = entityStore;
        }

        @Override
        public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
                                            ProjectionFactory factory, NamedQueries namedQueries) {

            InMemoryQueryMethod queryMethod = new InMemoryQueryMethod(method, metadata, factory);
            String namedQueryName = queryMethod.getNamedQueryName();

            if (!namedQueries.hasQuery(namedQueryName)) {
                throw new IllegalStateException(
                        "No named query " + namedQueryName + " declared for " + method.toString());
            }

            return new StringBasedInMemoryQuery(namedQueries.getQuery(namedQueryName),
                    queryMethod, entityStore);
        }
    }

    private static class CreateQueryLookupStrategy implements QueryLookupStrategy {

        private final InMemoryEntityStores entityStore;

        public CreateQueryLookupStrategy(InMemoryEntityStores entityStore) {
            this.entityStore = entityStore;
        }

        @Override
        public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
                                            ProjectionFactory factory, NamedQueries namedQueries) {

            InMemoryQueryMethod queryMethod = new InMemoryQueryMethod(method, metadata, factory);
            return new PartTreeInMemoryQuery(queryMethod, entityStore);
        }
    }
}