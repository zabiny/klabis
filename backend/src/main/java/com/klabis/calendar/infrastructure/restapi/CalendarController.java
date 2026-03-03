package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.application.CalendarManagementPort;
import com.klabis.calendar.application.CreateCalendarItemCommand;
import com.klabis.calendar.application.InvalidCalendarQueryException;
import com.klabis.calendar.application.UpdateCalendarItemCommand;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
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

import com.klabis.common.mvc.MvcComponent;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(value = "/api/calendar-items", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Calendar", description = "Calendar item management API")
@PrimaryAdapter
@ExposesResourceFor(CalendarItem.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.CALENDAR_SCOPE})
class CalendarController {

    private final CalendarManagementPort calendarManagementService;

    public CalendarController(CalendarManagementPort calendarManagementService) {
        this.calendarManagementService = calendarManagementService;
    }

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

        List<CalendarItemDto> items = calendarManagementService.listCalendarItems(effectiveStartDate, effectiveEndDate, sortObj)
                .stream().map(this::toDto).toList();

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

        addMonthNavigationLinks(collectionModel, effectiveStartDate, effectiveEndDate, sort);

        return ResponseEntity.ok(collectionModel);
    }

    private void addMonthNavigationLinks(CollectionModel<EntityModel<CalendarItemDto>> collectionModel,
                                          LocalDate currentStartDate,
                                          LocalDate currentEndDate,
                                          String sort) {
        LocalDate nextMonthStart = currentStartDate.plusMonths(1).withDayOfMonth(1);
        LocalDate nextMonthEnd = nextMonthStart.withDayOfMonth(nextMonthStart.lengthOfMonth());
        collectionModel.add(
                klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(nextMonthStart, nextMonthEnd, sort))
                        .withRel("next")
        );

        LocalDate prevMonthStart = currentStartDate.minusMonths(1).withDayOfMonth(1);
        LocalDate prevMonthEnd = prevMonthStart.withDayOfMonth(prevMonthStart.lengthOfMonth());
        collectionModel.add(
                klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(prevMonthStart, prevMonthEnd, sort))
                        .withRel("prev")
        );
    }

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

    @GetMapping("/{id}")
    @Operation(
            summary = "Get calendar item by ID",
            description = "Retrieves detailed calendar item information by ID. " +
                          "Returns HATEOAS links based on whether the item is manually created or event-linked."
    )
    @ApiResponse(responseCode = "200", description = "Calendar item found")
    public ResponseEntity<EntityModel<CalendarItemDto>> getCalendarItem(
            @Parameter(description = "Calendar item UUID") @PathVariable UUID id) {

        CalendarItemDto calendarItemDto = toDto(calendarManagementService.getCalendarItem(id));

        EntityModel<CalendarItemDto> entityModel = EntityModel.of(calendarItemDto);
        addLinksForCalendarItem(entityModel, calendarItemDto);

        return ResponseEntity.ok(entityModel);
    }

    @PostMapping(consumes = "application/json")
    @HasAuthority(Authority.CALENDAR_MANAGE)
    @Operation(
            summary = "Create a new manual calendar item",
            description = "Creates a new manual calendar item (not linked to an event). " +
                          "Manual items can be updated and deleted. " +
                          "Returns Location header pointing to the created resource."
    )
    @ApiResponse(responseCode = "201", description = "Calendar item successfully created")
    public ResponseEntity<Void> createCalendarItem(
            @Parameter(description = "Calendar item creation data")
            @Valid @RequestBody CreateCalendarItemCommand command) {

        UUID calendarItemId = calendarManagementService.createCalendarItem(command);

        return ResponseEntity
                .created(klabisLinkTo(methodOn(CalendarController.class).getCalendarItem(calendarItemId)).toUri())
                .build();
    }

    @PutMapping(value = "/{id}", consumes = "application/json")
    @HasAuthority(Authority.CALENDAR_MANAGE)
    @Operation(
            summary = "Update a manual calendar item",
            description = """
                    Updates calendar item information. Only allowed for manual items (not event-linked).
                    Event-linked items are read-only and managed automatically.
                    """
    )
    @ApiResponse(responseCode = "204", description = "Calendar item successfully updated")
    @ApiResponse(responseCode = "400", description = "Cannot update event-linked calendar item")
    public ResponseEntity<Void> updateCalendarItem(
            @Parameter(description = "Calendar item UUID") @PathVariable UUID id,
            @Parameter(description = "Calendar item update data") @Valid @RequestBody UpdateCalendarItemCommand command) {

        calendarManagementService.updateCalendarItem(id, command);
        return ResponseEntity.noContent().build();
    }

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

    private LocalDate getCurrentMonthFirstDay() {
        return LocalDate.now().withDayOfMonth(1);
    }

    private LocalDate getCurrentMonthLastDay() {
        return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
    }

    private CalendarItemDto toDto(CalendarItem calendarItem) {
        return new CalendarItemDto(
                calendarItem.getId().value(),
                calendarItem.getName(),
                calendarItem.getDescription(),
                calendarItem.getStartDate(),
                calendarItem.getEndDate(),
                calendarItem.getEventId() != null ? calendarItem.getEventId().value() : null
        );
    }

    private void addLinksForCalendarItem(EntityModel<?> entityModel, CalendarItemDto calendarItemDto) {
        UUID calendarItemId = calendarItemDto.id();
        boolean isEventLinked = calendarItemDto.eventId() != null;

        Link selfLink = klabisLinkTo(methodOn(CalendarController.class).getCalendarItem(calendarItemId)).withSelfRel();

        if (isEventLinked) {
            entityModel.add(selfLink);
            entityModel.add(Link.of("/api/events/" + calendarItemDto.eventId()).withRel("event"));
        } else {
            selfLink = selfLink.andAffordances(klabisAfford(methodOn(CalendarController.class).updateCalendarItem(calendarItemId, null)));
            selfLink = selfLink.andAffordances(klabisAfford(methodOn(CalendarController.class).deleteCalendarItem(calendarItemId)));
            entityModel.add(selfLink);
        }

        entityModel.add(klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()), "startDate,asc")).withRel("collection"));
    }
}

@MvcComponent
class CalendarRootPostprocessor implements org.springframework.hateoas.server.RepresentationModelProcessor<org.springframework.hateoas.EntityModel<com.klabis.common.ui.RootModel>> {

    @Override
    public org.springframework.hateoas.EntityModel<com.klabis.common.ui.RootModel> process(org.springframework.hateoas.EntityModel<com.klabis.common.ui.RootModel> model) {
        model.add(klabisLinkTo(methodOn(CalendarController.class).listCalendarItems(LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()), "startDate,asc")).withRel("calendar"));
        return model;
    }
}
