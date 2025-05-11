package club.klabis.adapters.oris;

import club.klabis.domain.events.EventsService;
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
class SynchronizationService {

    private static final Logger logger = LoggerFactory.getLogger(SynchronizationService.class);
    private final OrisApiClient orisApiClient;
    private final EventsService eventsService;

    SynchronizationService(OrisApiClient orisApiClient, EventsService eventsService) {
        this.orisApiClient = orisApiClient;
        this.eventsService = eventsService;
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT1H")
    void synchronizeEvents() {
        orisApiClient.getEventList(OrisApiClient.OrisEventListFilter.createDefault().withDateFrom(LocalDate.now().minusMonths(3)).withDateTo(LocalDate.now().plusMonths(3)))
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

        EventEditationForm form = new EventEditationForm(orisEvent.name(), orisEvent.location(), orisEvent.date(), orisEvent.organizer1().abbreviation(), getEntryDate(orisEvent).toLocalDate(), null);

        eventsService.findByOrisId(orisEvent.id())
                .ifPresentOrElse(event -> event.edit(form), () -> eventsService.createNewEvent(form));
    }
}
