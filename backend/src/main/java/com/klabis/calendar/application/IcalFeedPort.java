package com.klabis.calendar.application;

import com.klabis.events.domain.Event;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.time.LocalDate;
import java.util.List;

@PrimaryPort
public interface IcalFeedPort {

    /**
     * Retrieves events for the member's personal schedule within the default feed window.
     * <p>
     * The window is computed from {@code now}: [{@code now - past}, {@code now + future}]
     * where past and future are configured via {@code klabis.ical.window.*} properties.
     *
     * @return events paired with whether the member is the coordinator for each
     */
    List<EventScheduleEntry> getMySchedule(MemberId memberId, LocalDate now);

    record EventScheduleEntry(Event event, boolean isCoordinator) {}
}
