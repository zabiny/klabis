package com.klabis.events.management;

import com.klabis.common.users.UserId;
import com.klabis.events.Event;
import com.klabis.events.EventId;
import com.klabis.events.EventStatus;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.persistence.EventRepository;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for event management operations.
 * <p>
 * Handles event creation, updates, status transitions, and queries.
 * All mutation operations are transactional.
 */
@Service
@PrimaryPort
class EventManagementService {

    private final EventRepository eventRepository;

    public EventManagementService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Creates a new event in DRAFT status.
     *
     * @param command the create event command
     * @return the ID of the created event
     */
    @Transactional
    public UUID createEvent(CreateEventCommand command) {
        Event event = Event.create(
                command.name(),
                command.eventDate(),
                command.location(),
                command.organizer(),
                command.websiteUrl() != null ? WebsiteUrl.of(command.websiteUrl()) : null,
                command.eventCoordinatorId() != null ? new UserId(command.eventCoordinatorId()) : null
        );

        Event savedEvent = eventRepository.save(event);
        return savedEvent.getId().value();
    }

    /**
     * Updates an existing event.
     *
     * @param eventId the ID of the event to update
     * @param command the update event command
     * @throws EventNotFoundException if event not found
     * @throws IllegalStateException  if event is in FINISHED or CANCELLED status
     */
    @Transactional
    public void updateEvent(UUID eventId, UpdateEventCommand command) {
        Event event = eventRepository.findById(new EventId(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.update(
                command.name(),
                command.eventDate(),
                command.location(),
                command.organizer(),
                command.websiteUrl() != null ? WebsiteUrl.of(command.websiteUrl()) : null,
                command.eventCoordinatorId() != null ? new UserId(command.eventCoordinatorId()) : null
        );

        eventRepository.save(event);
    }

    /**
     * Publishes an event (DRAFT to ACTIVE transition).
     *
     * @param eventId the ID of the event to publish
     * @throws EventNotFoundException if event not found
     * @throws IllegalStateException  if transition is not allowed
     */
    @Transactional
    public void publishEvent(UUID eventId) {
        Event event = eventRepository.findById(new EventId(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.publish();
        eventRepository.save(event);
    }

    /**
     * Cancels an event (DRAFT/ACTIVE to CANCELLED transition).
     *
     * @param eventId the ID of the event to cancel
     * @throws EventNotFoundException if event not found
     * @throws IllegalStateException  if transition is not allowed
     */
    @Transactional
    public void cancelEvent(UUID eventId) {
        Event event = eventRepository.findById(new EventId(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.cancel();
        eventRepository.save(event);
    }

    /**
     * Finishes an event (ACTIVE to FINISHED transition).
     *
     * @param eventId the ID of the event to finish
     * @throws EventNotFoundException if event not found
     * @throws IllegalStateException  if transition is not allowed
     */
    @Transactional
    public void finishEvent(UUID eventId) {
        Event event = eventRepository.findById(new EventId(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));

        event.finish();
        eventRepository.save(event);
    }

    /**
     * Retrieves event details by ID.
     *
     * @param eventId the ID of the event
     * @return event details DTO
     * @throws EventNotFoundException if event not found
     */
    @Transactional(readOnly = true)
    public EventDto getEvent(UUID eventId) {
        Event event = eventRepository.findById(new EventId(eventId))
                .orElseThrow(() -> new EventNotFoundException(eventId));

        return mapToDto(event);
    }

    /**
     * Lists all events with pagination.
     *
     * @param pageable pagination and sorting parameters
     * @return page of event summaries
     */
    @Transactional(readOnly = true)
    public Page<EventSummaryDto> listEvents(Pageable pageable) {
        Page<Event> eventPage = eventRepository.findAll(pageable);
        return eventPage.map(this::mapToSummaryDto);
    }

    /**
     * Lists events filtered by status with pagination.
     *
     * @param status   the event status to filter by
     * @param pageable pagination and sorting parameters
     * @return page of event summaries
     */
    @Transactional(readOnly = true)
    public Page<EventSummaryDto> listEventsByStatus(EventStatus status, Pageable pageable) {
        Page<Event> eventPage = eventRepository.findByStatus(status, pageable);
        return eventPage.map(this::mapToSummaryDto);
    }

    // ========== Mapping Methods ==========

    /**
     * Maps Event domain object to EventDto.
     *
     * @param event the event to map
     * @return event DTO
     */
    private EventDto mapToDto(Event event) {
        return new EventDto(
                event.getId().value(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getWebsiteUrl() != null ? event.getWebsiteUrl().value() : null,
                event.getEventCoordinatorId() != null ? event.getEventCoordinatorId().uuid() : null,
                event.getStatus()
        );
    }

    /**
     * Maps Event domain object to EventSummaryDto.
     *
     * @param event the event to map
     * @return event summary DTO
     */
    private EventSummaryDto mapToSummaryDto(Event event) {
        return new EventSummaryDto(
                event.getId().value(),
                event.getName(),
                event.getEventDate(),
                event.getLocation(),
                event.getOrganizer(),
                event.getStatus()
        );
    }
}
