package club.klabis.adapters.inmemorystorage;

import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUsersRepository;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.MembersRepository;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("inmemorydb")
class RepositoryFactory {

    private final InMemoryRepositoryWithEventsFactory repoFactory;

    RepositoryFactory(InMemoryRepositoryWithEventsFactory repoFactory) {
        this.repoFactory = repoFactory;
    }

    @Bean
    public MembersRepository membersRepository() {
        return repoFactory.initializeRepositoryWithEventPublishingPostprocessing(MembersRepository.class, new MembersInMemoryRepository(), Member.class);
    }

    @Bean
    public ApplicationUsersRepository applicationUsersRepository() {
        return repoFactory.initializeRepositoryWithEventPublishingPostprocessing(ApplicationUsersRepository.class, new ApplicationUsersInMemoryRepository(), ApplicationUser.class);
    }

}
