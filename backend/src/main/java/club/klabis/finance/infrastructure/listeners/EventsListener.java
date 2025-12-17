package club.klabis.finance.infrastructure.listeners;

import club.klabis.events.domain.Event;
import club.klabis.events.domain.EventCostChangedEvent;
import club.klabis.events.domain.MemberEventRegistrationCreated;
import club.klabis.events.domain.MemberEventRegistrationRemoved;
import club.klabis.finance.domain.Account;
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

import java.util.function.Predicate;

@Service
public class EventsListener {

    private static final Logger logger = LoggerFactory.getLogger(EventsListener.class);

    private final EventsRepository eventsRepository;

    public EventsListener(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }


    @EventListener(MemberCreatedEvent.class)
    public void onMemberCreated(MemberCreatedEvent event) {
        MemberId accountOwner = event.getAggregate().getId();

        boolean accountExists = eventsRepository.project(new AccountProjector(accountOwner)).isPresent();

        if (accountExists) {
            logger.warn("Account already exists for member id " + accountOwner);
            return;
        }

        eventsRepository.appendEvent(new AccountCreatedEvent(accountOwner, MoneyAmount.ZERO));
    }


    @EventListener(EventCostChangedEvent.class)
    public void onEventCostChanged(EventCostChangedEvent event) {
        logger.warn("TODO: Add handling of event price change");
    }

    @EventListener(MemberEventRegistrationCreated.class)
    public void onEventRegistrationCreated(MemberEventRegistrationCreated event) {

        Event registeredEvent = event.getAggregate();

        registeredEvent.getCost()
                .filter(Predicate.not(MoneyAmount::isZero))
                .ifPresent(registeredEventCost -> {
                    MemberId registeredUser = event.getMemberId();

                    Account memberAccount = eventsRepository.project(new AccountProjector(registeredUser))
                            .orElseThrow();
                    memberAccount.registerPaymentForEvent(registeredEvent);

                    eventsRepository.appendPendingEventsFrom(memberAccount);
                });
    }

    @EventListener(MemberEventRegistrationRemoved.class)
    public void onEventRegistrationCanceled(MemberEventRegistrationRemoved event) {

        Event registeredEvent = event.getAggregate();
        // TODO: add event transaction item to refund what was paid only and to see money as reserved instead of paid
        registeredEvent.getCost()
                .filter(Predicate.not(MoneyAmount::isZero))
                .ifPresent(registeredEventCost -> {
                    MemberId registeredUser = event.getMemberId();

                    Account memberAccount = eventsRepository.project(new AccountProjector(registeredUser))
                            .orElseThrow();
                    memberAccount.refundEvent(registeredEvent);

                    eventsRepository.appendPendingEventsFrom(memberAccount);
                });
    }

}
