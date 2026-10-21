package com.klabis.calendar.application;

import com.klabis.events.EventData;
import com.klabis.events.EventId;
import com.klabis.events.application.EventDataProvider;
import com.klabis.events.application.EventScheduleQuery;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
class IcalFeedService implements IcalFeedPort {

    private final EventScheduleQuery eventScheduleQuery;
    private final EventDataProvider eventDataProvider;
    private final IcalWindowProperties windowProperties;

    IcalFeedService(EventScheduleQuery eventScheduleQuery, EventDataProvider eventDataProvider, IcalWindowProperties windowProperties) {
        this.eventScheduleQuery = eventScheduleQuery;
        this.eventDataProvider = eventDataProvider;
        this.windowProperties = windowProperties;
    }

    @Override
    public List<EventScheduleEntry> getMySchedule(MemberId memberId, LocalDate now) {
        Assert.notNull(memberId, "memberId must not be null");
        Assert.notNull(now, "now must not be null");

        LocalDate from = now.minus(windowProperties.getPast());
        LocalDate to = now.plus(windowProperties.getFuture());

        Set<EventId> eventIds = eventScheduleQuery.findEventIdsForMemberSchedule(memberId, from, to);

        if (eventIds.isEmpty()) {
            return List.of();
        }

        return eventDataProvider.getEventData(eventIds).stream()
                .map(event -> new EventScheduleEntry(event, event.isCoordinator(memberId)))
                .toList();
    }
}
