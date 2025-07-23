package club.klabis.adapters.inmemorystorage;

import club.klabis.application.events.EventsRepository;
import com.dpolach.inmemoryrepository.EnableInMemoryRepositories;
import com.dpolach.inmemoryrepository.InMemoryEntityStore;
import com.dpolach.inmemoryrepository.InMemoryTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("inmemorydb")
@EnableInMemoryRepositories(basePackageClasses = EventsRepository.class)
@EnableTransactionManagement
class RepositoryFactory {

    @Bean
    public InMemoryEntityStore entityStore() {
        return new InMemoryEntityStore();
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new InMemoryTransactionManager(entityStore());
    }

    @Bean
    @Scope("prototype")
    public TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager());
    }
}
