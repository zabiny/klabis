package com.klabis.events.application;

import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventStatus;
import com.klabis.events.domain.MemberRegistrationBlockRepository;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
class MemberRegistrationSanctionService implements MemberRegistrationSanctionPort {

    private static final Logger log = LoggerFactory.getLogger(MemberRegistrationSanctionService.class);

    private final EventRepository eventRepository;
    private final MemberRegistrationBlockRepository blockRepository;

    MemberRegistrationSanctionService(EventRepository eventRepository,
                                      MemberRegistrationBlockRepository blockRepository) {
        this.eventRepository = eventRepository;
        this.blockRepository = blockRepository;
    }

    @Override
    public void applyMissedSelectionSanction(MemberId memberId) {
        log.warn("Applying missed fee selection sanction for member {}", memberId);

        blockRepository.block(memberId);

        autoUnregisterFromOpenEvents(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMemberBlocked(MemberId memberId) {
        return blockRepository.isBlocked(memberId);
    }

    private void autoUnregisterFromOpenEvents(MemberId memberId) {
        EventFilter filter = EventFilter.byStatus(EventStatus.ACTIVE)
                .withRegisteredBy(memberId);

        Page<Event> activeRegistrations = eventRepository.findAll(filter, Pageable.unpaged());

        List<Event> eventsWithOpenRegistrations = activeRegistrations.stream()
                .filter(Event::areRegistrationsOpen)
                .filter(event -> event.findRegistration(memberId).isPresent())
                .toList();

        if (eventsWithOpenRegistrations.isEmpty()) {
            log.info("No open registrations found for sanctioned member {}", memberId);
            return;
        }

        log.warn("Auto-unregistering member {} from {} event(s) with open registrations. " +
                 "Manual restoration may be required after emergency fee assignment.",
                memberId, eventsWithOpenRegistrations.size());

        for (Event event : eventsWithOpenRegistrations) {
            log.warn("Auto-unregistering member {} from event {} ({})", memberId, event.getId(), event.getName());
            event.unregisterMember(new Event.UnregisterMember(memberId));
            eventRepository.save(event);
        }
    }
}
