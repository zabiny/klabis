package club.klabis.adapters.api.events;

import club.klabis.api.EventsApi;
import club.klabis.api.dto.EventListItemApiDto;
import club.klabis.api.dto.GetEvents200ResponseApiDto;
import club.klabis.domain.events.Event;
import club.klabis.domain.events.EventsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class EventsApiImpl implements EventsApi {
    private final EventsRepository eventsRepository;

    EventsApiImpl(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
    }


    @Override
    public ResponseEntity<GetEvents200ResponseApiDto> getEvents() {
        List<EventListItemApiDto> items = eventsRepository.findAll()
                .stream().map(this::fromOrisEvent).toList();

        return ResponseEntity.ok(new GetEvents200ResponseApiDto().items(items));
    }

    private EventListItemApiDto fromOrisEvent(Event oriEvent) {
        return new EventListItemApiDto()
                .id(oriEvent.getId().value())
                .date(oriEvent.getDate())
                .name(oriEvent.getName())
                .type(EventListItemApiDto.TypeEnum.S)
                .organizer(oriEvent.getOrganizer())
                .coordinator("")
                .registrationDeadline(oriEvent.getRegistrationDeadline());
    }

}
