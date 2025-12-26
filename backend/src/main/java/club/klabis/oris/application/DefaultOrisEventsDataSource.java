package club.klabis.oris.application;

import club.klabis.events.domain.OrisId;
import club.klabis.events.oris.OrisEventDataSource;
import club.klabis.events.oris.dto.OrisData;
import club.klabis.events.oris.dto.OrisDataBuilder;
import club.klabis.events.oris.dto.OrisEventListFilter;
import club.klabis.oris.application.dto.*;
import club.klabis.oris.infrastructure.apiclient.OrisApiClient;
import club.klabis.shared.application.OrisIntegrationComponent;
import jakarta.validation.constraints.NotNull;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SecondaryAdapter
@OrisIntegrationComponent
class DefaultOrisEventsDataSource implements OrisEventDataSource {

    private static final Logger logger = LoggerFactory.getLogger(DefaultOrisEventsDataSource.class);
    private final OrisApiClient orisApiClient;

    // For example Relay has different format of EventEntries (= 1 EventEntry contains one relay with multiple runners - that is something what we can't process now).
    private List<Integer> SUPPORTED_ORIS_DISCIPLINES = Arrays.asList(OrisApiClient.DISCIPLINE_ID_LONG_DISTANCE,
            OrisApiClient.DISCIPLINE_ID_SHORT_DISTANCE,
            OrisApiClient.DISCIPLINE_ID_SPRINT,
            OrisApiClient.DISCIPLINE_ID_ULTRALONG_DISTANCE);

    DefaultOrisEventsDataSource(OrisApiClient orisApiClient) {
        this.orisApiClient = orisApiClient;
    }

    private boolean isSupported(EventSummary event) {
        return SUPPORTED_ORIS_DISCIPLINES.contains(event.discipline().id());
    }

    private boolean isSupported(EventDetails event) {
        return SUPPORTED_ORIS_DISCIPLINES.contains(event.discipline().id());
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
                .registrations(eventRegistrations(eventDetails))
                .build();
    }

    private @NotNull Collection<OrisData.MemberRegistration> eventRegistrations(EventDetails event) {
        return orisApiClient.getEventEntries(event.id(), OrisApiClient.CLUB_ID_ZBM).payload()
                .map(data -> data.values().stream().map(this::from).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private OrisData.MemberRegistration from(EventEntry eventEntry) {
        return new OrisData.MemberRegistration(eventEntry.regNo(),
                eventEntry.classDesc(),
                eventEntry.si(),
                eventEntry.fee());
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
        return orisApiClient.getEventDetails(orisEventId.value()).payload().filter(this::isSupported).map(this::from);
    }

    private EventDetails toDetails(EventSummary event) {
        // throwing if not found because when we have summary, there should be also detail information available
        return orisApiClient.getEventDetails(event.id()).payload().orElseThrow();
    }

    @Override
    public Stream<OrisData> streamOrisEvents(OrisEventListFilter filter) {
        OrisApiClient.OrisResponse<Map<String, EventSummary>> response = orisApiClient.getEventList(filter);
        if (response.data() != null) {
            return response.data()
                    .values()
                    .stream()
                    .filter(this::isSupported)
                    .map(this::toDetails)
                    .map(this::from);
        } else {
            return Stream.empty();
        }
    }
}
