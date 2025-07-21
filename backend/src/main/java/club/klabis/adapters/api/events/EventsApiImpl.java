package club.klabis.adapters.api.events;

import club.klabis.api.EventsApi;
import club.klabis.api.dto.EventListItemApiDto;
import club.klabis.api.dto.GetEvents200ResponseApiDto;
import club.klabis.domain.events.Event;
import club.klabis.domain.events.EventsRepository;
import club.klabis.domain.events.forms.EventRegistrationForm;
import club.klabis.domain.members.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
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
                .stream().map(this::toListDto).toList();

        return ResponseEntity.ok(new GetEvents200ResponseApiDto().items(items));
    }

    private EventListItemApiDto toListDto(Event oriEvent) {
        return new EventListItemApiDto()
                .id(oriEvent.getId().value())
                .date(oriEvent.getDate())
                .name(oriEvent.getName())
                //.type(EventListItemApiDto.TypeEnum.S)
                .organizer(oriEvent.getOrganizer())
                //.coordinator("")
                .registrationDeadline(oriEvent.getRegistrationDeadline());
    }

    private EventListItemApiDto toDetailDto(Event event) {
        return toListDto(event);
    }

    @Operation(
            operationId = "registerMemberForEvent",
            summary = "Registers member to event",
            tags = {"events", "WIP"},
            parameters = {
                    @Parameter(name = "eventId", description = "ID události", required = true, in = ParameterIn.PATH, schema = @Schema(type = "integer"))
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Member was registered to event successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "401", description = "Missing required user authentication or authentication failed"),
                    @ApiResponse(responseCode = "403", description = "User is not allowed to perform requested operation"),
                    @ApiResponse(responseCode = "404", description = "Event with given doesn't exist")
            },
            security = {
                    @SecurityRequirement(name = "klabis", scopes = {"openid"})
            }
    )
    @RequestMapping(
            method = RequestMethod.POST,
            value = "/events/{eventId}/registrations",
            produces = {"application/json"}
    )
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    void registerMemberToEvent(@PathVariable(name = "eventId") int event, @RequestBody EventRegistrationForm form) {
        Event e = eventsRepository.findById(new Event.Id(event))
                .orElseThrow();

        e.addEventRegistration(form);
        eventsRepository.save(e);
    }

    @RequestMapping(
            method = RequestMethod.POST,
            value = "/member/{memberId}/registeredEvents",
            produces = {"application/json"}
    )
    @Transactional
    Collection<EventListItemApiDto> getRegisteredEventsForMember(@PathVariable(name = "memberId") int memberId) {
        return eventsRepository.findEventsByParticipantsIsContaining(new Member.Id(memberId)).stream().map(this::toDetailDto).toList();
    }

}
