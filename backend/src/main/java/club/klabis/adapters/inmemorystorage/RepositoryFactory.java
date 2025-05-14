package club.klabis.adapters.inmemorystorage;

import club.klabis.domain.DomainEventBase;
import com.dpolach.inmemoryrepository.EnableInMemoryRepositories;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("inmemorydb")
@EnableInMemoryRepositories(basePackageClasses = DomainEventBase.class)
class RepositoryFactory {

}
