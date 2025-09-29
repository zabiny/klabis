package club.klabis.shared.config.inmemorystorage;

import com.dpolach.inmemoryrepository.EnableInMemoryRepositories;
import com.dpolach.inmemoryrepository.InMemoryEntityStores;
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
@EnableInMemoryRepositories(basePackages = "club.klabis")
@EnableTransactionManagement
class RepositoryFactory {

    @Bean
    public InMemoryEntityStores entityStore() {
        return new InMemoryEntityStores();
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
