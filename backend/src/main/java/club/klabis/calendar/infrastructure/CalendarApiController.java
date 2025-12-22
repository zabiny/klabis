package club.klabis.calendar.infrastructure;

import club.klabis.calendar.Calendar;
import club.klabis.calendar.CalendarItem;
import club.klabis.calendar.CreateCalendarItemCommand;
import club.klabis.calendar.EventCalendarItem;
import club.klabis.events.domain.Event;
import club.klabis.shared.config.Globals;
import club.klabis.shared.config.hateoas.ModelAssembler;
import club.klabis.shared.config.hateoas.RootModel;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

import java.time.LocalDate;
import java.util.ArrayList;

import static club.klabis.shared.config.hateoas.forms.KlabisHateoasImprovements.affordBetter;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@ApiController(openApiTagName = "calendar", path = "/calendar-items")
@ExposesResourceFor(CalendarItem.class)
@Import(CalendarItemListPostprocessor.class)
public class CalendarApiController {

    private final CalendarService calendarService;
    private final ModelAssembler<CalendarItem, CalendarItemDto> modelAssembler;
    private final EntityLinks entityLinks;

    CalendarApiController(CalendarService calendarService, PagedResourcesAssembler<CalendarItem> pagedResourcesAssembler, EntityLinks entityLinks) {
        this.calendarService = calendarService;
        this.modelAssembler = ModelAssembler.mappingAssembler(this::toDto, CalendarApiController::toSelfLink,
                pagedResourcesAssembler);
        this.entityLinks = entityLinks;
    }

    @Relation(collectionRelation = "calendarItems")
    public record CalendarItemDto(LocalDate start, LocalDate end, String note, @JsonIgnore Link relatedItem) {
    }

    private CalendarItemDto toDto(CalendarItem item) {
        Link relatedItemLink = null;
        if (item instanceof EventCalendarItem eventCalendarItem) {
            relatedItemLink = entityLinks.linkForItemResource(Event.class, eventCalendarItem.getEventId())
                    .withRel("event");
        }

        return new CalendarItemDto(Globals.toLocalDate(item.getStart()),
                Globals.toLocalDate(item.getEnd()),
                item.getNote(), relatedItemLink);
    }

    private static Link toSelfLink(CalendarItem calendar) {
        return linkTo(methodOn(CalendarApiController.class).getCalendarItems(calendar.getId())).withSelfRel();
    }

    // TODO: Rework this endpoint to return Calendar instance with information about period, start date, end date, etc..  It will help with couple of things (displaying some stats in calendar, navigating calendar - as it is weird to have there links like prev/next on collection of items which may be empty... )
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<CalendarItemDto>>> getCalendarItems(@RequestParam(required = false, defaultValue = "MONTH") Calendar.CalendarType calendarType, @RequestParam(required = false) LocalDate referenceDate) {
        return ResponseEntity.ok(modelAssembler.toCollectionModel(new ArrayList<>(calendarService.getCalendarItems(
                calendarType,
                referenceDate))));
    }

    @GetMapping("/{id}")
    public EntityModel<CalendarItemDto> getCalendarItems(@PathVariable CalendarItem.Id id) {
        CalendarItem result = calendarService.getCalendarItem(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Calendar item with id %s was not found".formatted(id)));
        return modelAssembler.toEntityResponse(result);
    }

    @HasGrant(ApplicationGrant.CALENDAR_MANAGE)
    @PostMapping
    public ResponseEntity<Void> createCalendarItem(@RequestBody CreateCalendarItemCommand command) {
        CalendarItem item = calendarService.createCalendarItem(command);

        return ResponseEntity.created(toSelfLink(item).toUri()).build();
    }

}

@Component
@Order(4)
class CalendarRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        model.add(linkTo(methodOn(CalendarApiController.class).getCalendarItems(null,
                null)).withRel("calendar").expand("", ""));

        return model;
    }
}

@Component
class CalendarItemPostprocessor implements RepresentationModelProcessor<EntityModel<CalendarApiController.CalendarItemDto>> {

    @Override
    public EntityModel<CalendarApiController.CalendarItemDto> process(EntityModel<CalendarApiController.CalendarItemDto> model) {
        if (model.getContent().relatedItem() != null) {
            model.add(model.getContent().relatedItem());
        }

        return model;
    }
}

@Component
class CalendarItemListPostprocessor implements RepresentationModelProcessor<CollectionModel<EntityModel<CalendarApiController.CalendarItemDto>>> {
    @Override
    public CollectionModel<EntityModel<CalendarApiController.CalendarItemDto>> process(CollectionModel<EntityModel<CalendarApiController.CalendarItemDto>> model) {
        // TODO: add missing parameter values from current request to have proper "self" link
        model.add(linkTo(methodOn(CalendarApiController.class).getCalendarItems(null, null)).withSelfRel()
                .expand("", "")
                .andAffordances(affordBetter(methodOn(CalendarApiController.class).createCalendarItem(null))));

        model.add(linkTo(methodOn(CalendarApiController.class).getCalendarItems(Calendar.CalendarType.DAY,
                null)).withRel("calendar-day").expand(""));
        model.add(linkTo(methodOn(CalendarApiController.class).getCalendarItems(Calendar.CalendarType.YEAR,
                null)).withRel("calendar-year").expand(""));
        model.add(linkTo(methodOn(CalendarApiController.class).getCalendarItems(Calendar.CalendarType.MONTH,
                null)).withRel("calendar-month").expand(""));

        return model;
    }
}

@JacksonComponent
class IdConverter extends ValueSerializer<CalendarItem.Id> implements Converter<CalendarItem.Id, String> {

    @Override
    public void serialize(CalendarItem.Id value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        ctxt.findValueSerializer(Long.class).serialize(value != null ? value.value() : null, gen, ctxt);
    }

    @Override
    public @Nullable String convert(CalendarItem.Id source) {
        return source != null ? Long.toString(source.value()) : null;
    }
}

@JacksonComponent
class IdConverterDeser extends ValueDeserializer<CalendarItem.Id> implements Converter<String, CalendarItem.Id> {

    @Override
    public CalendarItem.Id deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        Long val = p.readValueAs(Long.class);
        if (val != null) {
            return new CalendarItem.Id(val);
        } else {
            return null;
        }
    }

    @Override
    public CalendarItem.Id convert(String source) {
        if (source == null) {
            return null;
        }
        return new CalendarItem.Id(Long.parseLong(source));
    }
}