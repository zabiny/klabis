package club.klabis.finance.infrastructure.listeners;

import club.klabis.finance.domain.Accounts;
import club.klabis.finance.domain.MoneyAmount;
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
        MemberId memberId = event.getAggregate().getId();

        Accounts accounts = eventsRepository.rebuild(new Accounts());

        if (accounts.getAccount(memberId).isPresent()) {
            logger.warn("Account already exists for member id " + memberId);
            return;
        }

        logger.debug("Creating finance account for created member %s".formatted(memberId));
        accounts.createAccount(memberId, MoneyAmount.ZERO);

        eventsRepository.appendPendingEventsFrom(accounts);
    }

}
