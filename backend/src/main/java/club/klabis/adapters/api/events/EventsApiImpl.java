package club.klabis.adapters.api.events;

import club.klabis.adapters.oris.OrisApiClient;
import club.klabis.api.EventsApi;
import club.klabis.api.dto.EventListItemApiDto;
import club.klabis.api.dto.GetEvents200ResponseApiDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@RestController
class EventsApiImpl implements EventsApi {
    private final OrisApiClient orisApiClient;

    EventsApiImpl(OrisApiClient orisApiClient) {
        this.orisApiClient = orisApiClient;
    }

    @Override
    public ResponseEntity<GetEvents200ResponseApiDto> getEvents() {
        // docasne vracime ciste eventy z ORIS
        List<EventListItemApiDto> items = orisApiClient.getEventList(OrisApiClient.OrisEventListFilter.createDefault().withDateFrom(LocalDate.now().minusMonths(3)).withDateTo(LocalDate.now().plusMonths(3)))
                .data().values().stream().map(this::fromOrisEvent).toList();

        return ResponseEntity.ok(new GetEvents200ResponseApiDto().items(items));
    }

    private EventListItemApiDto fromOrisEvent(OrisApiClient.OrisEvent oriEvent) {
        return new EventListItemApiDto()
                .id(oriEvent.id() * 2)
                .date(oriEvent.date())
                .name(oriEvent.name())
                .type(EventListItemApiDto.TypeEnum.S)
                .organizer(oriEvent.organizer1().abbreviation())
                .coordinator(null)
                .registrationDeadline(getEntryDate(oriEvent).toLocalDate())
                .web("https://oris.orientacnisporty.cz/Zavod?id=%s".formatted(oriEvent.id()));
    }

    private LocalDateTime getEntryDate(OrisApiClient.OrisEvent orisEvent) {
        return Stream.of(orisEvent.entryDate1(), orisEvent.entryDate2(), orisEvent.entryDate3())
                .filter(Objects::nonNull)
                .sorted()
                .filter(LocalDateTime.now()::isAfter)
                .findAny()
                .orElse(orisEvent.date().minusDays(3).atStartOfDay());
    }
}
