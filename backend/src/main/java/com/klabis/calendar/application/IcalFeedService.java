package com.klabis.calendar.application;

import com.klabis.events.EventId;
import com.klabis.events.EventScheduleQuery;
import com.klabis.events.domain.Events;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
class IcalFeedService implements IcalFeedPort {

    private final EventScheduleQuery eventScheduleQuery;
    private final Events events;
    private final IcalWindowProperties windowProperties;

    IcalFeedService(EventScheduleQuery eventScheduleQuery, Events events, IcalWindowProperties windowProperties) {
        this.eventScheduleQuery = eventScheduleQuery;
        this.events = events;
        this.windowProperties = windowProperties;
    }

    @Override
    public List<EventScheduleEntry> getMySchedule(MemberId memberId, LocalDate now) {
        Assert.notNull(memberId, "memberId must not be null");
        Assert.notNull(now, "now must not be null");

        LocalDate from = now.minus(windowProperties.getPast());
        LocalDate to = now.plus(windowProperties.getFuture());

        Set<EventId> eventIds = eventScheduleQuery.findEventIdsForMemberSchedule(memberId, from, to);

        return eventIds.stream()
                .map(events::findById)
                .flatMap(java.util.Optional::stream)
                .map(event -> {
                    boolean isCoordinator = memberId.equals(event.getEventCoordinatorId());
                    return new EventScheduleEntry(event, isCoordinator);
                })
                .toList();
    }
}
