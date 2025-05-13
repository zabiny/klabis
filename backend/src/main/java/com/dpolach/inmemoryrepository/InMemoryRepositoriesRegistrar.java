package com.dpolach.inmemoryrepository;

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;

import java.lang.annotation.Annotation;

public class InMemoryRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableInMemoryRepositories.class;
    }

    @Override
    protected RepositoryConfigurationExtension getExtension() {
        return new InMemoryRepositoryConfigurationExtension();
    }
}

class InMemoryRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    @Override
    public String getModuleName() {
        return "inmemoryrepository";
    }

    @Override
    protected String getModulePrefix() {
        return "com.dpolach";
    }

    @Override
    public String getRepositoryFactoryBeanClassName() {
        return InMemoryRepositoryFactory.class.getName();
    }

    // Další požadované metody
}