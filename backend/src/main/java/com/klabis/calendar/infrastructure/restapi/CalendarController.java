package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.common.users.Authority;
import com.klabis.common.users.authorization.HasAuthority;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
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

    public CalendarController(CalendarManagementService calendarManagementService) {
        this.calendarManagementService = calendarManagementService;
    }

    /**
     * List calendar items with date range filtering.
     * <p>
     * GET /api/calendar-items?startDate={date}&endDate={date}&sort=startDate,asc
     * <p>
     * If startDate and endDate are not provided, defaults to current month.
     * Maximum date range is 1 year (366 days).
     *
     * @param startDate start date for filtering (inclusive, defaults to first day of current month)
     * @param endDate   end date for filtering (inclusive, defaults to last day of current month)
     * @param sort      sorting parameters (default: startDate,asc)
     * @return collection of calendar item summaries
     */
    @GetMapping
    @Operation(
            summary = "List calendar items with date range filtering",
            description = """
                    Retrieves a list of calendar items filtered by date range.
                    If dates not provided, defaults to current month.
                    Maximum date range is 1 year (366 days).
                    Default sort: startDate,asc. Allowed fields: id, name, startDate, endDate.
                    """
    )
    @ApiResponse(responseCode = "200", description = "List of calendar items retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Date range exceeds 366 days or invalid sort field")
    public ResponseEntity<CollectionModel<EntityModel<CalendarItemDto>>> listCalendarItems(
            @Parameter(description = "Start date for filtering (ISO DATE format, defaults to first day of current month)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date for filtering (ISO DATE format, defaults to last day of current month)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Sorting parameters (default: startDate,asc)")
            @RequestParam(defaultValue = "startDate,asc") String sort) {

        LocalDate effectiveStartDate = startDate != null ? startDate : getCurrentMonthFirstDay();
        LocalDate effectiveEndDate = endDate != null ? endDate : getCurrentMonthLastDay();

        Sort sortObj = parseAndValidateSort(sort);

        List<CalendarItemDto> items = calendarManagementService.listCalendarItems(effectiveStartDate, effectiveEndDate, sortObj);

        CollectionModel<EntityModel<CalendarItemDto>> collectionModel = CollectionModel.of(
                items.stream()
                        .map(dto -> {
                            EntityModel<CalendarItemDto> model = EntityModel.of(dto);
                            model.add(klabisLinkTo(methodOn(CalendarController.class).getCalendarItem(dto.id())).withSelfRel());
                            return model;
                        })
                        .toList()
        );

        Link selfLink = klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(effectiveStartDate, effectiveEndDate, sort))
                .withSelfRel()
                .andAffordances(klabisAfford(methodOn(CalendarController.class).createCalendarItem(null)));

        collectionModel.add(selfLink);

        // Add next/prev month navigation links
        addMonthNavigationLinks(collectionModel, effectiveStartDate, effectiveEndDate, sort);

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Adds next and prev month navigation links to the collection model.
     *
     * @param collectionModel the collection model to add links to
     * @param currentStartDate current date range start
     * @param currentEndDate   current date range end
     * @param sort             sort parameter to preserve
     */
    private void addMonthNavigationLinks(CollectionModel<EntityModel<CalendarItemDto>> collectionModel,
                                          LocalDate currentStartDate,
                                          LocalDate currentEndDate,
                                          String sort) {
        // Add next month link
        LocalDate nextMonthStart = currentStartDate.plusMonths(1).withDayOfMonth(1);
        LocalDate nextMonthEnd = nextMonthStart.withDayOfMonth(nextMonthStart.lengthOfMonth());
        collectionModel.add(
                klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(nextMonthStart, nextMonthEnd, sort))
                        .withRel("next")
        );

        // Add prev month link
        LocalDate prevMonthStart = currentStartDate.minusMonths(1).withDayOfMonth(1);
        LocalDate prevMonthEnd = prevMonthStart.withDayOfMonth(prevMonthStart.lengthOfMonth());
        collectionModel.add(
                klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(prevMonthStart, prevMonthEnd, sort))
                        .withRel("prev")
        );
    }

    /**
     * Parses and validates sort parameter.
     *
     * @param sort sort parameter (format: "field,direction" or "field")
     * @return Sort object
     * @throws InvalidCalendarQueryException if sort field is invalid
     */
    private Sort parseAndValidateSort(String sort) {
        final var allowedSortFields = java.util.Set.of("id", "name", "startDate", "endDate");

        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = Sort.Direction.ASC;

        if (parts.length > 1) {
            String dir = parts[1].trim();
            direction = switch (dir.toLowerCase()) {
                case "desc" -> Sort.Direction.DESC;
                case "asc" -> Sort.Direction.ASC;
                default -> throw new InvalidCalendarQueryException(
                        "Invalid sort direction: " + dir + ". Must be 'asc' or 'desc'"
                );
            };
        }

        if (!allowedSortFields.contains(field)) {
            throw new InvalidCalendarQueryException(
                    "Invalid sort field: " + field + ". Allowed fields: " + allowedSortFields
            );
        }

        return Sort.by(direction, field);
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
        entityModel.add(klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()), "startDate,asc")).withRel("collection"));
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
        model.add(klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()), "startDate,asc")).withRel("calendar"));
        return model;
    }
}
