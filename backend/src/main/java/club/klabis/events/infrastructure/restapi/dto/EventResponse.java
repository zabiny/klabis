package club.klabis.events.infrastructure.restapi.dto;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.EventManagementCommand;
import club.klabis.shared.config.restapi.ResponseViews;
import com.fasterxml.jackson.annotation.*;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.hateoas.server.core.Relation;

import java.util.List;
import java.util.Optional;

/**
 * Event response DTO
 */
@Relation("event")
@JsonTypeName("Event")
@RecordBuilder
public record EventResponse(@JsonIgnore Event source,
                            @JsonProperty(access = JsonProperty.Access.READ_ONLY) Event.Id id,
                            @JsonProperty(access = JsonProperty.Access.READ_ONLY) TypeEnum type,
                            @JsonProperty(access = JsonProperty.Access.READ_ONLY) String web,
                            @JsonProperty(access = JsonProperty.Access.READ_ONLY) @JsonView(ResponseViews.Detailed.class) List<EventRegistrationResponse> registrations,
                            @JsonUnwrapped EventManagementCommand managementForm

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

