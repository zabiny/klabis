package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.application.CalendarManagementPort;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.EventCalendarItem;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.ui.ModelWithDomainPostprocessor;
import com.klabis.common.ui.RootModel;
import com.klabis.events.EventId;
import com.klabis.events.infrastructure.restapi.EventsApi;
import com.klabis.members.ActingUser;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@PrimaryAdapter
@ExposesResourceFor(CalendarItem.class)
class CalendarController implements CalendarApi {

    private final CalendarManagementPort calendarManagementService;

    public CalendarController(CalendarManagementPort calendarManagementService) {
        this.calendarManagementService = calendarManagementService;
    }

    @Override
    public ResponseEntity<List<CalendarItemDto>> listCalendarItems(
            LocalDate startDate,
            LocalDate endDate,
            String sort,
            Boolean mySchedule,
            @ActingUser CurrentUserData currentUser) {

        LocalDate effectiveStartDate = startDate != null ? startDate : getCurrentMonthFirstDay();
        LocalDate effectiveEndDate = endDate != null ? endDate : getCurrentMonthLastDay();

        Sort sortObj = parseAndValidateSort(sort);

        boolean myScheduleRequested = Boolean.TRUE.equals(mySchedule);
        MemberId myScheduleMemberId = myScheduleRequested && currentUser != null ? currentUser.memberId() : null;

        List<CalendarItem> calendarItems = calendarManagementService
                .listCalendarItems(effectiveStartDate, effectiveEndDate, sortObj, myScheduleRequested, myScheduleMemberId);

        List<CalendarItemDto> payload = calendarItems.stream().map(this::toDto).toList();

        HalResponseContext.setDomainList(calendarItems);
        return ResponseEntity.ok(payload);
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
                default -> throw new IllegalArgumentException(
                        "Invalid sort direction: " + dir + ". Must be 'asc' or 'desc'"
                );
            };
        }

        if (!allowedSortFields.contains(field)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + field + ". Allowed fields: " + allowedSortFields
            );
        }

        return Sort.by(direction, field);
    }

    @Override
    public ResponseEntity<CalendarItemDto> getCalendarItem(
            UUID id) {

        CalendarItem calendarItem = calendarManagementService.getCalendarItem(new CalendarItemId(id));

        HalResponseContext.setDomain(calendarItem);
        return ResponseEntity.ok(toDto(calendarItem));
    }

    @Override
    public ResponseEntity<Void> createCalendarItem(
            CreateCalendarItemRequest request) {

        CalendarItem created = calendarManagementService.createCalendarItem(toCommand(request));

        return ResponseEntity
                .created(klabisLinkTo(methodOn(CalendarApi.class).getCalendarItem(created.getId().value()))
                        .map(link -> link.toUri())
                        .orElseGet(() -> URI.create("/api/calendar-items/" + created.getId().value())))
                .build();
    }

    @Override
    public ResponseEntity<Void> updateCalendarItem(
            UUID id,
            UpdateCalendarItemRequest request) {

        calendarManagementService.updateCalendarItem(new CalendarItemId(id), toCommand(request));
        return ResponseEntity.noContent().build();
    }

    private static CalendarItem.CreateCalendarItem toCommand(CreateCalendarItemRequest request) {
        String description = normalizeDescription(request.description());
        return new CalendarItem.CreateCalendarItem(request.name(), description, request.startDate(), request.endDate());
    }

    private static CalendarItem.UpdateCalendarItem toCommand(UpdateCalendarItemRequest request) {
        String description = normalizeDescription(request.description());
        return new CalendarItem.UpdateCalendarItem(request.name(), description, request.startDate(), request.endDate());
    }

    private static String normalizeDescription(String description) {
        return description != null && description.isBlank() ? null : description;
    }

    @Override
    public ResponseEntity<Void> deleteCalendarItem(
            UUID id) {

        calendarManagementService.deleteCalendarItem(new CalendarItemId(id));
        return ResponseEntity.noContent().build();
    }

    private LocalDate getCurrentMonthFirstDay() {
        return LocalDate.now().withDayOfMonth(1);
    }

    private LocalDate getCurrentMonthLastDay() {
        return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
    }

    private CalendarItemDto toDto(CalendarItem calendarItem) {
        var eventId = calendarItem instanceof EventCalendarItem linked ? linked.getEventId() : null;
        return new CalendarItemDto(
                calendarItem.getId(),
                calendarItem.getName(),
                calendarItem.getDescription(),
                calendarItem.getStartDate(),
                calendarItem.getEndDate(),
                eventId
        );
    }

}

@MvcComponent
class CalendarItemPostprocessor extends ModelWithDomainPostprocessor<CalendarItemDto, CalendarItem> {

    @Override
    public void process(EntityModel<CalendarItemDto> dtoModel, CalendarItem calendarItem) {
        UUID calendarItemId = calendarItem.getId().value();

        klabisLinkTo(methodOn(CalendarApi.class).getCalendarItem(calendarItemId)).ifPresent(selfLinkBuilder -> {
            if (calendarItem instanceof EventCalendarItem eventItem) {
                dtoModel.add(selfLinkBuilder.withSelfRel());
                EventId eventId = eventItem.getEventId();
                klabisLinkTo(methodOn(EventsApi.class).getEvent(eventId.value(), null))
                        .ifPresent(link -> dtoModel.add(link.withRel("event")));
            } else {
                var selfLink = selfLinkBuilder.withSelfRel()
                        .andAffordances(klabisAfford(methodOn(CalendarApi.class).updateCalendarItem(calendarItemId, null)))
                        .andAffordances(klabisAfford(methodOn(CalendarApi.class).deleteCalendarItem(calendarItemId)));
                dtoModel.add(selfLink);
            }
        });

        LocalDate today = LocalDate.now();
        klabisLinkTo(methodOn(CalendarApi.class).listCalendarItems(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()), "startDate,asc", null, null))
                .ifPresent(link -> dtoModel.add(link.withRel("collection")));
    }
}

/**
 * Adds the collection-level create affordance plus month navigation links. The self link itself is
 * built by {@code HalResponseBodyAdvice} from the current request (preserving startDate, endDate,
 * sort and mySchedule), so this processor only contributes to it and adds next/prev.
 */
@MvcComponent
class CalendarItemListPostprocessor
        implements RepresentationModelProcessor<CollectionModel<EntityModel<CalendarItemDto>>> {

    @Override
    public CollectionModel<EntityModel<CalendarItemDto>> process(
            CollectionModel<EntityModel<CalendarItemDto>> model) {
        model.mapLink(IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(CalendarApi.class).createCalendarItem(null))));

        model.getLink(IanaLinkRelations.SELF)
                .map(Link::getHref)
                .ifPresent(selfHref -> addMonthNavigationLinks(model, selfHref));

        return model;
    }

    private void addMonthNavigationLinks(CollectionModel<EntityModel<CalendarItemDto>> model, String selfHref) {
        var uriComponents = org.springframework.web.util.UriComponentsBuilder.fromUriString(selfHref).build();
        var params = uriComponents.getQueryParams();

        LocalDate currentStartDate = params.getFirst("startDate") != null
                ? LocalDate.parse(params.getFirst("startDate")) : getCurrentMonthFirstDay();
        LocalDate currentEndDate = params.getFirst("endDate") != null
                ? LocalDate.parse(params.getFirst("endDate")) : getCurrentMonthLastDay();
        String sort = params.getFirst("sort") != null ? params.getFirst("sort") : "startDate,asc";
        Boolean mySchedule = params.getFirst("mySchedule") != null ? Boolean.valueOf(params.getFirst("mySchedule")) : null;

        LocalDate nextMonthStart = currentStartDate.plusMonths(1).withDayOfMonth(1);
        LocalDate nextMonthEnd = nextMonthStart.withDayOfMonth(nextMonthStart.lengthOfMonth());
        klabisLinkTo(methodOn(CalendarApi.class).listCalendarItems(nextMonthStart, nextMonthEnd, sort, mySchedule, null))
                .ifPresent(link -> model.add(link.withRel("next")));

        LocalDate prevMonthStart = currentStartDate.minusMonths(1).withDayOfMonth(1);
        LocalDate prevMonthEnd = prevMonthStart.withDayOfMonth(prevMonthStart.lengthOfMonth());
        klabisLinkTo(methodOn(CalendarApi.class).listCalendarItems(prevMonthStart, prevMonthEnd, sort, mySchedule, null))
                .ifPresent(link -> model.add(link.withRel("prev")));
    }

    private LocalDate getCurrentMonthFirstDay() {
        return LocalDate.now().withDayOfMonth(1);
    }

    private LocalDate getCurrentMonthLastDay() {
        return LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
    }
}

@MvcComponent
class CalendarRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(CalendarApi.class).listCalendarItems(
                LocalDate.now().withDayOfMonth(1),
                LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
                "startDate,asc", null, null
        )).ifPresent(link -> model.add(link.withRel("calendar")));
        return model;
    }
}
