package club.klabis.events.infrastructure.restapi.dto;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.members.infrastructure.restapi.ResponseViews;
import com.fasterxml.jackson.annotation.*;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Event response DTO
 */
@Relation("event")
@JsonTypeName("Event")
@RecordBuilder
public record EventResponse(@JsonIgnore Event source, Event.Id id,
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, String name,
                            String location, String organizer, TypeEnum type,
                            String web, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate registrationDeadline,
                            Integer coordinator,
                            @JsonView(ResponseViews.Detailed.class) List<EventRegistrationResponse> registrations

) {

    /**
     * Gets or Sets type
     */
    public enum TypeEnum {
        TRAINING,

        COMPETITION;

        @JsonValue
        public String getJsonValue() {
            return name();
        }

        @JsonCreator
        public static TypeEnum fromJsonValue(String value) {
            return TypeEnum.valueOf(value.toUpperCase());
        }

        public static Optional<TypeEnum> ofEvent(Event event) {
            return Optional.ofNullable(event).map(e -> {
                if (event instanceof Competition) {
                    return EventResponse.TypeEnum.COMPETITION;
                }

                return null;
            });
        }
    }
}

