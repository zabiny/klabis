package club.klabis.adapters.oris;

import club.klabis.domain.members.events.MemberCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class RegisterMemberListener {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterMemberListener.class);

    @EventListener(MemberCreatedEvent.class)
    public void onMemberCreated(MemberCreatedEvent event) {
        LOG.warn("TODO: create new club member in ORIS - registration id = %s".formatted(event.getAggregate().getRegistration()));
    }
}
