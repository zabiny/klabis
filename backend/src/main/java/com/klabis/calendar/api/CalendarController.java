package com.klabis.calendar.api;

import com.klabis.calendar.CalendarItem;
import com.klabis.users.Authority;
import com.klabis.users.authorization.HasAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller for CalendarItem resources.
 * <p>
 * Provides HATEOAS-compliant endpoints for calendar item management.
 * All mutation operations require CALENDAR:MANAGE authority.
 * Read operations are available to any authenticated member.
 */
@RestController
@RequestMapping(value = "/api/calendar-items", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Calendar", description = "Calendar item management API")
@PrimaryAdapter
@ExposesResourceFor(CalendarItem.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.CALENDAR_SCOPE})
class CalendarController {

    private final CalendarManagementService calendarManagementService;
    private final PagedResourcesAssembler<CalendarItemDto> pagedResourcesAssembler;

    public CalendarController(
            CalendarManagementService calendarManagementService,
            PagedResourcesAssembler<CalendarItemDto> pagedResourcesAssembler) {
        this.calendarManagementService = calendarManagementService;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    /**
     * List calendar items with date range filtering.
     * <p>
     * GET /api/calendar-items?startDate={date}&endDate={date}&page=0&size=10
     * <p>
     * If startDate and endDate are not provided, defaults to current month.
     *
     * @param startDate start date for filtering (inclusive, defaults to first day of current month)
     * @param endDate   end date for filtering (inclusive, defaults to last day of current month)
     * @param pageable  pagination and sorting parameters
     * @return paginated collection of calendar item summaries
     */
    @GetMapping
    @Operation(
            summary = "List calendar items with pagination and date range filtering",
            description = """
                    Retrieves a paginated list of calendar items.
                    Supports filtering by date range.
                    If dates not provided, defaults to current month.
                    Default: page=0, size=20, sort=startDate,asc.
                    Allowed sort fields: id, name, startDate, endDate.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Paginated list of calendar items retrieved successfully")
    public ResponseEntity<PagedModel<EntityModel<CalendarItemDto>>> listCalendarItems(
            @Parameter(description = "Start date for filtering (defaults to first day of current month)")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "End date for filtering (defaults to last day of current month)")
            @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Pagination parameters: page, size, sort")
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.ASC) @ParameterObject Pageable pageable) {

        LocalDate effectiveStartDate = startDate != null ? startDate : getCurrentMonthFirstDay();
        LocalDate effectiveEndDate = endDate != null ? endDate : getCurrentMonthLastDay();

        validateSortFields(pageable.getSort());

        Page<CalendarItemDto> page = calendarManagementService.listCalendarItems(effectiveStartDate, effectiveEndDate, pageable);

        PagedModel<EntityModel<CalendarItemDto>> pagedModel = pagedResourcesAssembler.toModel(
                page,
                dto -> {
                    EntityModel<CalendarItemDto> model = EntityModel.of(dto);
                    model.add(klabisLinkTo(methodOn(CalendarController.class).getCalendarItem(dto.id())).withSelfRel());
                    return model;
                }
        );

        Link selfLink = klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(effectiveStartDate, effectiveEndDate, pageable))
                .withSelfRel()
                .andAffordances(klabisAfford(methodOn(CalendarController.class).createCalendarItem(null)));

        pagedModel.mapLink(IanaLinkRelations.SELF, link -> selfLink);

        return ResponseEntity.ok(pagedModel);
    }

    /**
     * Get calendar item by ID.
     * <p>
     * GET /api/calendar-items/{id}
     *
     * @param id calendar item ID
     * @return calendar item resource with full details
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Get calendar item by ID",
            description = "Retrieves detailed calendar item information by ID. " +
                          "Returns HATEOAS links based on whether the item is manually created or event-linked."
    )
    @ApiResponse(responseCode = "200", description = "Calendar item found")
    public ResponseEntity<EntityModel<CalendarItemDto>> getCalendarItem(
            @Parameter(description = "Calendar item UUID") @PathVariable UUID id) {

        CalendarItemDto calendarItemDto = calendarManagementService.getCalendarItem(id);

        EntityModel<CalendarItemDto> entityModel = EntityModel.of(calendarItemDto);
        addLinksForCalendarItem(entityModel, calendarItemDto);

        return ResponseEntity.ok(entityModel);
    }

    /**
     * Create a new manual calendar item.
     * <p>
     * POST /api/calendar-items
     *
     * @param command create calendar item command
     * @return 201 Created with Location header and calendar item resource
     */
    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.CALENDAR_MANAGE)
    @Operation(
            summary = "Create a new manual calendar item",
            description = "Creates a new manual calendar item (not linked to an event). " +
                          "Manual items can be updated and deleted. " +
                          "Returns HATEOAS links for resource navigation."
    )
    @ApiResponse(responseCode = "201", description = "Calendar item successfully created")
    public ResponseEntity<EntityModel<CalendarItemDto>> createCalendarItem(
            @Parameter(description = "Calendar item creation data")
            @Valid @RequestBody CreateCalendarItemCommand command) {

        UUID calendarItemId = calendarManagementService.createCalendarItem(command);
        CalendarItemDto calendarItemDto = calendarManagementService.getCalendarItem(calendarItemId);

        EntityModel<CalendarItemDto> entityModel = EntityModel.of(calendarItemDto);
        addLinksForCalendarItem(entityModel, calendarItemDto);

        return ResponseEntity
                .created(klabisLinkTo(methodOn(CalendarController.class).getCalendarItem(calendarItemId)).toUri())
                .body(entityModel);
    }

    /**
     * Update a manual calendar item.
     * <p>
     * PUT /api/calendar-items/{id}
     *
     * @param id      calendar item ID
     * @param command update calendar item command
     * @return 200 OK with updated calendar item resource
     */
    @PutMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.CALENDAR_MANAGE)
    @Operation(
            summary = "Update a manual calendar item",
            description = """
                    Updates calendar item information. Only allowed for manual items (not event-linked).
                    Event-linked items are read-only and managed automatically.
                    Returns HATEOAS links for resource navigation.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Calendar item successfully updated")
    @ApiResponse(responseCode = "400", description = "Cannot update event-linked calendar item")
    public ResponseEntity<EntityModel<CalendarItemDto>> updateCalendarItem(
            @Parameter(description = "Calendar item UUID") @PathVariable UUID id,
            @Parameter(description = "Calendar item update data") @Valid @RequestBody UpdateCalendarItemCommand command) {

        calendarManagementService.updateCalendarItem(id, command);
        CalendarItemDto calendarItemDto = calendarManagementService.getCalendarItem(id);

        EntityModel<CalendarItemDto> entityModel = EntityModel.of(calendarItemDto);
        addLinksForCalendarItem(entityModel, calendarItemDto);

        return ResponseEntity.ok(entityModel);
    }

    /**
     * Delete a manual calendar item.
     * <p>
     * DELETE /api/calendar-items/{id}
     *
     * @param id calendar item ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @HasAuthority(Authority.CALENDAR_MANAGE)
    @Operation(
            summary = "Delete a manual calendar item",
            description = "Deletes a manual calendar item. " +
                          "Only allowed for manual items (not event-linked). " +
                          "Event-linked items are read-only and managed automatically."
    )
    @ApiResponse(responseCode = "204", description = "Calendar item successfully deleted")
    @ApiResponse(responseCode = "400", description = "Cannot delete event-linked calendar item")
    public ResponseEntity<Void> deleteCalendarItem(
            @Parameter(description = "Calendar item UUID") @PathVariable UUID id) {

        calendarManagementService.deleteCalendarItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the first day of the current month.
     *
     * @return first day of current month
     */
    private LocalDate getCurrentMonthFirstDay() {
        return LocalDate.now().withDayOfMonth(1);
    }

    /**
     * Returns the last day of the current month.
     *
     * @return last day of current month
     */
    private LocalDate getCurrentMonthLastDay() {
        return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
    }

    /**
     * Validates that all sort fields are in the allowed list.
     *
     * @param sort the sort specification to validate
     * @throws InvalidCalendarQueryException if any sort field is not allowed
     */
    private void validateSortFields(Sort sort) {
        final var allowedSortFields = java.util.Set.of(
                "id",
                "name",
                "startDate",
                "endDate"
        );

        for (Sort.Order order : sort) {
            if (!allowedSortFields.contains(order.getProperty())) {
                throw new InvalidCalendarQueryException(
                        "Invalid sort field: " + order.getProperty() +
                        ". Allowed fields: " + allowedSortFields
                );
            }
        }
    }

    /**
     * Add HATEOAS links to calendar item based on whether it's manually created or event-linked.
     *
     * @param entityModel      the entity model to add links to
     * @param calendarItemDto  the calendar item DTO
     */
    private void addLinksForCalendarItem(EntityModel<?> entityModel, CalendarItemDto calendarItemDto) {
        UUID calendarItemId = calendarItemDto.id();
        boolean isEventLinked = calendarItemDto.eventId() != null;

        Link selfLink = klabisLinkTo(methodOn(CalendarController.class).getCalendarItem(calendarItemId)).withSelfRel();

        if (isEventLinked) {
            // Event-linked items: read-only, add link to event
            entityModel.add(selfLink);
            entityModel.add(Link.of("/api/events/" + calendarItemDto.eventId()).withRel("event"));
        } else {
            // Manual items: editable, add edit and delete affordances
            selfLink = selfLink.andAffordances(klabisAfford(methodOn(CalendarController.class).updateCalendarItem(calendarItemId, null)));
            selfLink = selfLink.andAffordances(klabisAfford(methodOn(CalendarController.class).deleteCalendarItem(calendarItemId)));
            entityModel.add(selfLink);
        }

        // Collection link - always present
        entityModel.add(klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(null, null, Pageable.unpaged())).withRel("collection"));
    }
}

/**
 * HATEOAS processor that adds calendar link to root navigation.
 * <p>
 * Adds link to calendar items collection at the API root for frontend navigation.
 */
@org.springframework.stereotype.Component
class CalendarRootPostprocessor implements org.springframework.hateoas.server.RepresentationModelProcessor<org.springframework.hateoas.EntityModel<com.klabis.common.ui.RootModel>> {

    @Override
    public org.springframework.hateoas.EntityModel<com.klabis.common.ui.RootModel> process(org.springframework.hateoas.EntityModel<com.klabis.common.ui.RootModel> model) {
        model.add(klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(null, null, org.springframework.data.domain.Pageable.unpaged())).withRel("calendar"));
        return model;
    }
}
