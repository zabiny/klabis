package club.klabis.adapters.oris;

import club.klabis.domain.members.events.MemberCreatedEvent;
import club.klabis.domain.members.events.MemberEditedEvent;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class MemberSyncToOrisListener {

    private static final Logger LOG = LoggerFactory.getLogger(MemberSyncToOrisListener.class);

    @DomainEventHandler
    @EventListener(MemberCreatedEvent.class)
    public void onMemberCreated(MemberCreatedEvent event) {
        LOG.warn("TODO: create new club member in ORIS - registration id = %s".formatted(event.getAggregate().getRegistration()));
    }

    @DomainEventHandler
    @EventListener(MemberEditedEvent.class)
    public void onMemberEdited(MemberEditedEvent event) {
        LOG.warn("TODO: update club member data in ORIS - registration id = %s".formatted(event.getAggregate().getRegistration()));
    }

}
