package club.klabis.oris.application;

import club.klabis.events.domain.OrisId;
import club.klabis.events.oris.OrisEventDataSource;
import club.klabis.events.oris.dto.OrisData;
import club.klabis.events.oris.dto.OrisDataBuilder;
import club.klabis.events.oris.dto.OrisEventListFilter;
import club.klabis.oris.application.dto.EventClass;
import club.klabis.oris.application.dto.EventDetails;
import club.klabis.oris.application.dto.Organizer;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@SecondaryAdapter
class DefaultOrisEventsDataSource implements OrisEventDataSource {

    private static final Logger logger = LoggerFactory.getLogger(DefaultOrisEventsDataSource.class);
    private final OrisDataProvider orisDataProvider;

    DefaultOrisEventsDataSource(OrisDataProvider orisDataProvider) {
        this.orisDataProvider = orisDataProvider;
    }

    private OrisData from(EventDetails eventDetails) {
        return OrisDataBuilder.builder()
                .name(eventDetails.name())
                .orisId(new OrisId(eventDetails.id()))
                .categories(toCategories(eventDetails))
                .location(eventDetails.place())
                .organizer(formatOrganizer(eventDetails.org1(), eventDetails.org2()))
                .eventDate(eventDetails.date())
                .registrationsDeadline((ZonedDateTime) maximal(eventDetails.entryDate1(),
                        eventDetails.entryDate2(),
                        eventDetails.entryDate3()).orElse(eventDetails.date().atStartOfDay(ZoneId.of("Europe/Prague"))))
                .website(new OrisId(eventDetails.id()).createEventUrl())
                .build();
    }

    private Collection<String> toCategories(EventDetails eventDetails) {
        if (eventDetails.classes() != null && !eventDetails.classes().isEmpty()) {
            return toCategories(eventDetails.classes().values());
        }
        return Collections.emptyList();
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

    @Override
    public Optional<OrisData> getOrisEventData(OrisId orisEventId) {
        return orisDataProvider.getEventDetails(orisEventId).map(this::from);
    }

    @Override
    public Stream<OrisData> streamOrisEvents(OrisEventListFilter filter) {
        return orisDataProvider.getEventList(filter).stream()
                .flatMap(eventSummary -> orisDataProvider.getEventDetails(new OrisId(eventSummary.id())).stream())
                .map(this::from);
    }
}
