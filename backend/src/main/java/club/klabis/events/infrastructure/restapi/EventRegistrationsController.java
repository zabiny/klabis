package club.klabis.events.infrastructure.restapi;

import club.klabis.events.application.EventRegistrationUseCase;
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
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@Tag(name = "Event registrations")
@SecurityRequirement(name = "klabis", scopes = {"openapi"})
@RestController
@RequestMapping(produces = "application/json")
public class EventRegistrationsController {
    private final EventRegistrationUseCase useCase;

    public EventRegistrationsController(EventRegistrationUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(
            operationId = "getRegistrationForm",
            summary = "Returns data for registration form",
            parameters = {
                    @Parameter(name = "eventId", description = "ID události", required = true, in = ParameterIn.PATH, schema = @Schema(type = "integer")),
                    @Parameter(name = "memberId", description = "ID clena", required = true, in = ParameterIn.PATH, schema = @Schema(type = "integer"))
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Prepared event registration form for member and event"),
                    @ApiResponse(responseCode = "400", description = "Invalid request"),
                    @ApiResponse(responseCode = "401", description = "Missing required user authentication or authentication failed"),
                    @ApiResponse(responseCode = "403", description = "User is not allowed to perform requested operation"),
                    @ApiResponse(responseCode = "404", description = "Event with given doesn't exist")
            }
    )
    @GetMapping("/events/{eventId}/registrationForms/{memberId}")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<EventRegistrationForm> getEventRegistrationForm(@PathVariable(name = "eventId") int event, @PathVariable(name = "memberId") int memberId) {
        return ResponseEntity.ok(useCase.createEventRegistrationForm(new Event.Id(event), new MemberId(memberId)));
    }

    @Operation(
            operationId = "submitRegistrationForm",
            summary = "Submits registration form - registers member to event",
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
    @PutMapping("/events/{eventId}/registrationForms/{memberId}")
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<Void> submitRegistrationForm(@PathVariable(name = "eventId") int event, @PathVariable(name = "memberId") int memberId, @RequestBody EventRegistrationForm form) {
        useCase.registerForEvent(new Event.Id(event), new MemberId(memberId), form);
        return ResponseEntity.created(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(this.getClass())
                .getEventRegistrationForm(event, memberId)).toUri()).build();
    }

    @DeleteMapping("/events/{eventId}/registrationForms/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    ResponseEntity<Void> cancelEventRegistration(@PathVariable(name = "eventId") int eventId, @PathVariable(name = "memberId") int memberId) {
        useCase.cancelMemberRegistration(new Event.Id(eventId), new MemberId(memberId));
        return ResponseEntity.noContent().build();
    }
}
