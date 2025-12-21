package club.klabis.events.infrastructure.restapi;

import club.klabis.events.application.EventRegistrationUseCase;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.MemberId;
import club.klabis.shared.config.hateoas.HalResourceAssembler;
import club.klabis.shared.config.hateoas.ModelAssembler;
import club.klabis.shared.config.hateoas.ModelPreparator;
import club.klabis.shared.config.restapi.ApiController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import org.springframework.hateoas.mediatype.hal.forms.ImprovedHalFormsAffordanceModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@ApiController(path = "/events/{eventId}/registrationForms/{memberId}", openApiTagName = "Event registrations")
class EventRegistrationsController {
    private final EventRegistrationUseCase useCase;

    private final ModelAssembler<RegistrationData, EventRegistrationUseCase.EventRegistrationFormData> modelAssembler;

    EventRegistrationsController(EventRegistrationUseCase useCase, ModelPreparator<RegistrationData, EventRegistrationUseCase.EventRegistrationFormData> preparator, PagedResourcesAssembler<RegistrationData> pageableAssembler) {
        this.useCase = useCase;
        this.modelAssembler = new HalResourceAssembler<>(preparator, pageableAssembler);
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
    EntityModel<EventRegistrationUseCase.EventRegistrationFormData> getEventRegistrationForm(@PathVariable(name = "eventId") Event.Id event, @PathVariable(name = "memberId") MemberId memberId) {

        EventRegistrationUseCase.EventRegistrationFormData form = useCase.getEventRegistrationForm(event, memberId);

        return modelAssembler.toEntityResponse(new RegistrationData(event, memberId, form));
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

record RegistrationData(Event.Id eventId, MemberId memberId, EventRegistrationUseCase.EventRegistrationFormData form) {
}

@Component
class EventRegistrationModelPreparator implements ModelPreparator<RegistrationData, EventRegistrationUseCase.EventRegistrationFormData> {

    @Override
    public EventRegistrationUseCase.EventRegistrationFormData toResponseDto(RegistrationData registrationData) {
        return registrationData.form();
    }

    @Override
    public void addLinks(EntityModel<EventRegistrationUseCase.EventRegistrationFormData> resource, RegistrationData registrationData) {
        ModelPreparator.super.addLinks(resource, registrationData);

        Link selfLink = linkTo(methodOn(EventRegistrationsController.class).getEventRegistrationForm(registrationData.eventId(),
                registrationData.memberId())).withSelfRel();

        selfLink = selfLink.andAffordances(affordBetter(methodOn(EventRegistrationsController.class).submitRegistrationForm(
                registrationData.eventId(),
                registrationData.memberId(),
                null), affordance -> this.addOptions(affordance, registrationData)));

        resource.add(selfLink);
    }

    private void addOptions(ImprovedHalFormsAffordanceModel affordance, RegistrationData registrationData) {
        HalFormsOptions categoryOptions = HalFormsOptions.remote(linkTo(methodOn(EventsController.class).getEventCategories(
                        registrationData.eventId())).withRel("categories"))
                .withMaxItems(1L);

        affordance.defineOptions("category", categoryOptions);
    }

}