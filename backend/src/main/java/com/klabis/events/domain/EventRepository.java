package com.klabis.events.domain;

import com.klabis.events.EventId;
import com.klabis.members.MemberId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Internal repository interface for Event aggregate.
 * <p>
 * Defines persistence operations for the Events bounded context.
 * Implementation will be provided in the infrastructure layer.
 *
 * @apiNote This is internal API for use within the events module only.
 * Other modules should use {@link com.klabis.events.application.EventDataProvider}.
 */
@SecondaryPort
public interface EventRepository {

    Optional<Event> findById(EventId eventId);

    /**
     * @param filter   use {@link EventFilter#none()} for unfiltered results
     */
    Page<Event> findAll(EventFilter filter, Pageable pageable);

    /**
     * Saves an event to the repository.
     *
     * @param event the event to save
     * @return the saved event with generated ID
     */
    Event save(Event event);

    /**
     * Ids of all events (regardless of status) the member is listed as a coordinator of. Loads no aggregates,
     * so it is cheap enough to run for every authenticated request.
     */
    List<EventId> findIdsCoordinatedBy(MemberId memberId);

    /**
     * Those of the given event ids that belong to an ACTIVE event. Loads no aggregates.
     */
    List<EventId> findActiveIds(Collection<EventId> eventIds);

}
