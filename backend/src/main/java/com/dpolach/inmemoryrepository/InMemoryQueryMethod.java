package com.dpolach.inmemoryrepository;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

import java.lang.reflect.Method;

public class InMemoryQueryMethod extends QueryMethod {

    private final Method method;

    public InMemoryQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
        super(method, metadata, factory);
        this.method = method;
    }

    /**
     * Vrátí název pojmenovaného dotazu pro tuto metodu.
     */
    public String getNamedQueryName() {
        return getRepositoryName() + "." + method.getName();
    }

    private String getRepositoryName() {
        return method.getDeclaringClass().getSimpleName();
    }
}