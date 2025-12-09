package club.klabis.finance.infrastructure.listeners;

import club.klabis.finance.domain.AccountProjector;
import club.klabis.finance.domain.MoneyAmount;
import club.klabis.finance.domain.events.AccountCreatedEvent;
import club.klabis.members.MemberId;
import club.klabis.members.domain.events.MemberCreatedEvent;
import com.dpolach.eventsourcing.EventsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class EventsListener {

    private static final Logger logger = LoggerFactory.getLogger(EventsListener.class);

    private final EventsRepository eventsRepository;

    public EventsListener(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }


    @EventListener
    public void onMemberCreated(MemberCreatedEvent event) {
        MemberId accountOwner = event.getAggregate().getId();

        boolean accountExists = eventsRepository.project(new AccountProjector(accountOwner)).isPresent();

        if (accountExists) {
            logger.warn("Account already exists for member id " + accountOwner);
            return;
        }

        eventsRepository.appendEvent(new AccountCreatedEvent(accountOwner, MoneyAmount.ZERO));
    }

}
