package club.klabis.calendar.infrastructure;

import club.klabis.calendar.Calendar;
import club.klabis.calendar.CalendarItem;
import club.klabis.calendar.CreateCalendarItemCommand;
import club.klabis.shared.config.Globals;
import club.klabis.shared.config.hateoas.ModelAssembler;
import club.klabis.shared.config.hateoas.RootModel;
import club.klabis.shared.config.restapi.ApiController;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.boot.jackson.JacksonMixin;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
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
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@ApiController(openApiTagName = "calendar", path = "/calendar-items")
public class CalendarApiController {

    private final CalendarService calendarService;
    private final ModelAssembler<CalendarItem, CalendarItem> modelAssembler;

    CalendarApiController(CalendarService calendarService, PagedResourcesAssembler<CalendarItem> pagedResourcesAssembler) {
        this.calendarService = calendarService;
        this.modelAssembler = ModelAssembler.identityAssembler(CalendarApiController::toSelfLink,
                pagedResourcesAssembler);
    }

    private static Link toSelfLink(CalendarItem calendar) {
        return linkTo(methodOn(CalendarApiController.class).getCalendarItems(calendar.getId())).withSelfRel();
    }
//
//    @GetMapping
//    public ResponseEntity<CollectionModel<EntityModel<CalendarItem>>> getCalendarItems(@RequestParam Calendar.CalendarType calendarType, @RequestParam(required = false) LocalDate referenceDate) {
//        return ResponseEntity.ok(modelAssembler.toCollectionModel(calendarService.getCalendarItems(calendarType, referenceDate)));
//    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<CalendarItem>>> getCalendarItems(@RequestParam Calendar.CalendarType calendarType, @RequestParam(required = false) LocalDate referenceDate) {
        return ResponseEntity.ok(modelAssembler.toPagedResponse(new PageImpl<>(new ArrayList<>(calendarService.getCalendarItems(
                calendarType,
                referenceDate)))));
    }

    @GetMapping("/{id}")
    public EntityModel<CalendarItem> getCalendarItems(@PathVariable CalendarItem.Id id) {
        CalendarItem result = calendarService.getCalendarItem(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Calendar item with id %s was not found".formatted(id)));
        return modelAssembler.toEntityResponse(result);
    }

    @PostMapping
    public ResponseEntity<Void> createCalendarItem(@RequestBody CreateCalendarItemCommand command) {
        CalendarItem item = calendarService.createCalendarItem(command);

        return ResponseEntity.created(toSelfLink(item).toUri()).build();
    }

}

@Component
class CalendarRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {

    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        model.add(linkTo(methodOn(CalendarApiController.class).getCalendarItems(Calendar.CalendarType.MONTH,
                null)).withRel("calendar").expand(
                Map.of("referenceDate", "")));


        return model;
    }
}

@JacksonMixin(CalendarItem.class)
@JsonPropertyOrder({"id", "start", "end", "note"})
class CalendarItemMixin {
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = Globals.KLABIS_ZONE_VALUE)
    Object start;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = Globals.KLABIS_ZONE_VALUE)
    Object end;
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