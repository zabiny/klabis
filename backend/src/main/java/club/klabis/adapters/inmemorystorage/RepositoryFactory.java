package club.klabis.adapters.inmemorystorage;

import club.klabis.domain.members.Member;
import club.klabis.domain.members.MembersRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RepositoryFactory {

    private final InMemoryRepositoryWithEventsFactory repoFactory;

    RepositoryFactory(InMemoryRepositoryWithEventsFactory repoFactory) {
        this.repoFactory = repoFactory;
    }

    @Bean
    public MembersRepository membersRepository() {
        return repoFactory.decorateWithEventsPublisher(MembersRepository.class, new MembersInMemoryRepository(), Member.class);
    }

}
