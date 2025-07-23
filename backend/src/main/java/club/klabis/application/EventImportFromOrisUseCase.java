package club.klabis.application;

import club.klabis.adapters.oris.OrisApiClient;
import club.klabis.domain.events.Event;
import club.klabis.domain.events.forms.EventEditationForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;

@Service
class EventImportFromOrisUseCase {

    private static final Logger logger = LoggerFactory.getLogger(EventImportFromOrisUseCase.class);
    private final OrisApiClient orisApiClient;
    private final EventsRepository eventsRepository;

    EventImportFromOrisUseCase(OrisApiClient orisApiClient, EventsRepository eventsRepository) {
        this.orisApiClient = orisApiClient;
        this.eventsRepository = eventsRepository;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1H")
    void synchronizeEvents() {
        orisApiClient.getEventList(OrisApiClient.OrisEventListFilter.createDefault()
                        .withDateFrom(LocalDate.now().minusMonths(3))
                        .withDateTo(LocalDate.now().plusMonths(3)))
                .data().forEach(this::synchronizeEvent);
    }

    private LocalDateTime getEntryDate(OrisApiClient.OrisEvent orisEvent) {
        return Stream.of(orisEvent.entryDate1(), orisEvent.entryDate2(), orisEvent.entryDate3())
                .filter(Objects::nonNull)
                .sorted()
                .filter(LocalDateTime.now()::isAfter)
                .findAny()
                .orElse(orisEvent.date().minusDays(3).atStartOfDay());
    }

    private void synchronizeEvent(String id, OrisApiClient.OrisEvent orisEvent) {
        logger.info("Synchronizing event {}: {}", id, orisEvent);

        eventsRepository.findByOrisId(orisEvent.id())
                .ifPresentOrElse(event -> updateEventFromOris(event, orisEvent), () -> importOrisEvent(orisEvent));
    }

    private void updateEventFromOris(Event event, OrisApiClient.OrisEvent orisEvent) {
        EventEditationForm form = createEventEditationForm(orisEvent);
        event.edit(form);
        eventsRepository.save(event);
    }

    private void importOrisEvent(OrisApiClient.OrisEvent orisEvent) {
        EventEditationForm form = createEventEditationForm(orisEvent);
        Event event = Event.newEvent(form).linkWithOris(orisEvent.id());
        eventsRepository.save(event);
    }

    private EventEditationForm createEventEditationForm(OrisApiClient.OrisEvent orisEvent) {
        EventEditationForm form = new EventEditationForm(orisEvent.name(),
                orisEvent.location(),
                orisEvent.date(),
                orisEvent.organizer1().abbreviation(),
                getEntryDate(orisEvent).toLocalDate(),
                null);
        return form;
    }
}
