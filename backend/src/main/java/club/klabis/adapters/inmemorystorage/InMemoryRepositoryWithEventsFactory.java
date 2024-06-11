package club.klabis.adapters.inmemorystorage;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.EventPublishingRepositoryProxyPostProcessor;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.util.Streamable;
import org.springframework.data.util.TypeInformation;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Function;

/**
 * A bit hack-factory for in memory repositories which are populating Application events same way as repositories created by Spring Data module.
 *
 * This shall be replaced with proper Spring Data repositories later
 */
@Component
class InMemoryRepositoryWithEventsFactory {

    private final ApplicationEventPublisher publisher;

    public InMemoryRepositoryWithEventsFactory(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public <T, X extends T, D> T decorateWithEventsPublisher(Class<T> repoInterface, Object proxyTarget, Class<D> domainType) {
        EventPublishingRepositoryProxyPostProcessor processor = new EventPublishingRepositoryProxyPostProcessor(publisher);
        ProxyFactory factory = new ProxyFactory(proxyTarget);
        factory.addInterface(repoInterface);
        processor.postProcess(factory, new DomainInfoInRepositoryInformation(domainType));
        return (T) factory.getProxy();
    }

    // Creates repository using Proxy with default InMemoryRepositoryImpl. Usable for repositories without customized methods
    public <T extends ListCrudRepository<D, I>, D extends AbstractAggregateRoot<D>, I> T createInMemoryRepositoryWithEvents(Class<T> repoInterface, Class<D> domainType, Function<D, I> idExtractor) {
        InMemoryRepositoryImpl<D, I> target = new InMemoryRepositoryImpl<>(idExtractor);

        return decorateWithEventsPublisher(repoInterface, target, domainType);
    }

    /**
     * Hacky way how to pass information about domain object type into {@link EventPublishingRepositoryProxyPostProcessor#postProcess(ProxyFactory, RepositoryInformation)} method.
     */
    private class DomainInfoInRepositoryInformation implements RepositoryInformation {

        private final Class<?> domainType;

        DomainInfoInRepositoryInformation(Class<?> domainType) {
            this.domainType = domainType;
        }

        @Override
        public Class<?> getDomainType() {
            return domainType;
        }

        @Override
        public TypeInformation<?> getIdTypeInformation() {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public TypeInformation<?> getDomainTypeInformation() {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public Class<?> getRepositoryInterface() {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public TypeInformation<?> getReturnType(Method method) {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public Class<?> getReturnedDomainClass(Method method) {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public CrudMethods getCrudMethods() {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public boolean isPagingRepository() {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public Set<Class<?>> getAlternativeDomainTypes() {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public boolean isReactiveRepository() {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public Set<RepositoryFragment<?>> getFragments() {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public boolean isBaseClassMethod(Method method) {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public boolean isCustomMethod(Method method) {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public boolean isQueryMethod(Method method) {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public Streamable<Method> getQueryMethods() {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public Class<?> getRepositoryBaseClass() {
            throw new NotImplementedException("Not supporter");
        }

        @Override
        public Method getTargetClassMethod(Method method) {
            throw new NotImplementedException("Not supporter");
        }
    }

}
