package club.klabis.oris.application;

import club.klabis.events.application.OrisData;
import club.klabis.events.application.OrisDataBuilder;
import club.klabis.events.application.OrisSynchronizationUseCase;
import club.klabis.events.domain.Event;
import club.klabis.oris.application.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(prefix = "oris-integration", name = "enabled", havingValue = "true")
class DefaultOrisEventsImporter implements OrisEventsImporter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultOrisEventsImporter.class);
    private final OrisDataProvider orisDataProvider;
    private final OrisSynchronizationUseCase useCase;

    DefaultOrisEventsImporter(OrisDataProvider orisDataProvider, OrisSynchronizationUseCase useCase) {
        this.orisDataProvider = orisDataProvider;
        this.useCase = useCase;
    }

    @Override
    @Async
    public void loadOrisEvents(OrisEventListFilter filter) {
        orisDataProvider.getEventList(filter).forEach(this::synchronizeEvent);
    }

    @Override
    public void synchronizeEvents(Collection<Event.Id> eventIds) {
        useCase.getOrisIds(eventIds)
                .stream().map(orisDataProvider::getEventDetails)
                .flatMap(Optional::stream)
                .map(this::from)
                .forEach(useCase::importEvent);
    }

    private void synchronizeEvent(EventSummary eventSummary) {
        orisDataProvider.getEventDetails(eventSummary.id())
                .ifPresent(this::synchronizeEvent);
    }

    private void synchronizeEvent(EventDetails eventDetails) {
        OrisData orisData = this.from(eventDetails);
        useCase.importEvent(orisData);
    }

    private OrisData from(EventDetails eventDetails) {
        return OrisDataBuilder.builder()
                .name(eventDetails.name())
                .orisId(eventDetails.id())
                .categories(toCategories(eventDetails.classes().values()))
                .location(eventDetails.place())
                .organizer(formatOrganizer(eventDetails.org1(), eventDetails.org2()))
                .eventDate(eventDetails.date())
                .registrationsDeadline((ZonedDateTime) maximal(eventDetails.entryDate1(),
                        eventDetails.entryDate2(),
                        eventDetails.entryDate3()).orElse(eventDetails.date().atStartOfDay(
                        ZoneId.of("Europe/Prague"))))
                .build();
    }

    private Collection<String> toCategories(Collection<EventClass> classes) {
        return classes.stream().map(EventClass::name).collect(Collectors.toSet());
    }

    private static <T> Optional<T> firstNonNull(T... items) {
        return Arrays.stream(items).filter(Objects::nonNull).findFirst();
    }

    private static <T extends Comparable<T>> Optional<T> maximal(T... items) {
        return Arrays.stream(items).filter(Objects::nonNull).sorted().findFirst();
    }

    private static String formatOrganizer(Organizer... organizer) {
        return Arrays.stream(organizer)
                .filter(Objects::nonNull)
                .map(Organizer::abbreviation)
                .collect(Collectors.joining(" + "));
    }
}
