package club.klabis.events.adapters.restapi;

import club.klabis.events.adapters.restapi.dto.EventListItemApiDto;
import club.klabis.events.application.EventRegistrationUseCase;
import club.klabis.events.application.EventsRepository;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.MemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@Validated
@Tag(name = "Event registrations")
@SecurityRequirement(name = "klabis", scopes = {"openapi"})
@RestController
public class EventRegistrationsController {
    private final EventsRepository eventsRepository;
    private final EventRegistrationUseCase useCase;

    public EventRegistrationsController(EventsRepository eventsRepository, EventRegistrationUseCase useCase) {
        this.eventsRepository = eventsRepository;
        this.useCase = useCase;
    }

    @Operation(
            operationId = "registerMemberForEvent",
            summary = "Registers member to event",
            parameters = {
                    @Parameter(name = "eventId", description = "ID události", required = true, in = ParameterIn.PATH, schema = @Schema(type = "integer")),
                    @Parameter(name = "memberId", description = "ID clena", required = true, in = ParameterIn.PATH, schema = @Schema(type = "integer"))
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Member was registered to event successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "401", description = "Missing required user authentication or authentication failed"),
                    @ApiResponse(responseCode = "403", description = "User is not allowed to perform requested operation"),
                    @ApiResponse(responseCode = "404", description = "Event with given doesn't exist")
            }
    )
    @RequestMapping(
            method = RequestMethod.GET,
            value = "/events/{eventId}/registrationForm/{memberId}",
            produces = {"application/json"}
    )
    @ResponseStatus(HttpStatus.CREATED)
    EventRegistrationForm getEventRegistrationForm(@PathVariable(name = "eventId") int event, @PathVariable(name = "memberId") int memberId) {
        return useCase.createEventRegistrationForm(new Event.Id(event), new MemberId(memberId));
    }

    @Operation(
            operationId = "registerMemberForEvent",
            summary = "Registers member to event",
            parameters = {
                    @Parameter(name = "eventId", description = "ID události", required = true, in = ParameterIn.PATH, schema = @Schema(type = "integer")),
                    @Parameter(name = "memberId", description = "ID clena", required = true, in = ParameterIn.PATH, schema = @Schema(type = "integer"))
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "Member was registered to event successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "401", description = "Missing required user authentication or authentication failed"),
                    @ApiResponse(responseCode = "403", description = "User is not allowed to perform requested operation"),
                    @ApiResponse(responseCode = "404", description = "Event with given doesn't exist")
            }
    )
    @RequestMapping(
            method = RequestMethod.PUT,
            value = "/events/{eventId}/registrationForm/{memberId}",
            produces = {"application/json"}
    )
    @ResponseStatus(HttpStatus.CREATED)
    void submitRegistrationForm(@PathVariable(name = "eventId") int event, @PathVariable(name = "memberId") int memberId, @RequestBody EventRegistrationForm form) {
        useCase.registerForEvent(new Event.Id(event), new MemberId(memberId), form);
    }

    @RequestMapping(
            method = RequestMethod.DELETE,
            value = "/events/{eventId}/registrations/{memberId}",
            produces = {"application/json"}
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelEventRegistration(@PathVariable(name = "eventId") int eventId, @PathVariable(name = "memberId") int memberId) {
        useCase.cancelMemberRegistration(new Event.Id(eventId), new MemberId(memberId));
    }

    @RequestMapping(
            method = RequestMethod.GET,
            value = "/member/{memberId}/registeredEvents",
            produces = {"application/json"}
    )
    Collection<EventListItemApiDto> getMemberRegistrations(@PathVariable(name = "memberId") int memberId) {
        return eventsRepository.findEventsByRegistrationsContaining(new MemberId(memberId))
                .stream()
                .map(EventsController::toDetailDto)
                .toList();
    }

}
