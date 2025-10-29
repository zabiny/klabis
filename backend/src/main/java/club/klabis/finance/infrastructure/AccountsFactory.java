package club.klabis.finance.infrastructure;

import club.klabis.finance.domain.Accounts;
import com.dpolach.eventsourcing.EventsRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class AccountsFactory {

    private final EventsRepository eventsRepository;

    public AccountsFactory(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }

    @Bean
    @Scope("prototype")
    public Accounts getAccounts() {
        Accounts a = new Accounts();
        eventsRepository.findAll().forEach(a::apply);
        return a;
    }
}
