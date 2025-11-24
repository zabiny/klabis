package club.klabis.events.infrastructure.restapi;

import club.klabis.events.application.EventRegistrationUseCase;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.MemberId;
import club.klabis.shared.config.restapi.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.hateoas.*;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import org.springframework.hateoas.mediatype.hal.forms.ImprovedHalFormsAffordanceModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@ApiController(path = "/events/{eventId}/registrationForms/{memberId}", openApiTagName = "Event registrations")
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
    @GetMapping
    RepresentationModel<EntityModel<EventRegistrationUseCase.EventRegistrationFormData>> getEventRegistrationForm(@PathVariable(name = "eventId") Event.Id event, @PathVariable(name = "memberId") MemberId memberId) {

        EventRegistrationUseCase.EventRegistrationFormData form = useCase.getEventRegistrationForm(event, memberId);

        EntityModel<EventRegistrationUseCase.EventRegistrationFormData> result = EntityModel.of(form,
                linkTo(methodOn(EventRegistrationsController.class).getEventRegistrationForm(event, memberId))
                        .withSelfRel()
                        .andAffordance(affordBetter(methodOn(EventRegistrationsController.class).submitRegistrationForm(
                                event,
                                memberId,
                                null))));


        result.mapLink(LinkRelation.of("self"), l -> this.addOptions(l, event, memberId));

        return result;
    }

    @GetMapping("/categories")
    public List<String> getEventCategories(@PathVariable Event.Id eventId, @PathVariable MemberId memberId) {
        return useCase.getEventCategories(eventId);
    }

    private Link addOptions(Link link, Event.Id eventId, MemberId memberId) {
        HalFormsOptions categoryOptions = HalFormsOptions.remote(linkTo(methodOn(this.getClass()).getEventCategories(
                        eventId,
                        memberId)).withRel("categories"))
                .withMaxItems(1L);

        link.getAffordances().stream().map(a -> a.getAffordanceModel(MediaTypes.HAL_FORMS_JSON)).filter(
                        ImprovedHalFormsAffordanceModel.class::isInstance).map(ImprovedHalFormsAffordanceModel.class::cast)
                .forEach(a -> a.defineOptions("category", categoryOptions));
        return link;
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
    @PutMapping
    ResponseEntity<Void> submitRegistrationForm(@PathVariable(name = "eventId") Event.Id event, @PathVariable(name = "memberId") MemberId memberId, @RequestBody EventRegistrationForm form) {
        useCase.registerForEvent(event, memberId, form);
        return ResponseEntity.created(linkTo(methodOn(this.getClass())
                .getEventRegistrationForm(event, memberId)).toUri()).build();
    }

    @DeleteMapping
    ResponseEntity<Void> cancelEventRegistration(@PathVariable(name = "eventId") Event.Id eventId, @PathVariable(name = "memberId") MemberId memberId) {
        useCase.cancelMemberRegistration(eventId, memberId);
        return ResponseEntity.noContent().build();
    }
}
