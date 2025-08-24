package club.klabis.events.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDate;
import java.util.Objects;

/**
 * EventListItemApiDto
 */

@JsonTypeName("EventListItem")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class EventListItemApiDto extends RepresentationModel<EventListItemApiDto> {

    private Integer id;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private String name;

    private String organizer;

    /**
     * Gets or Sets type
     */
    public enum TypeEnum {
        T("T"),

        S("S");

        private String value;

        TypeEnum(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static TypeEnum fromValue(String value) {
            for (TypeEnum b : TypeEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private TypeEnum type;

    private String web;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registrationDeadline;

    private String coordinator;

    public EventListItemApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public EventListItemApiDto(Integer id, LocalDate date, String name, String organizer, TypeEnum type) {
        this.id = id;
        this.date = date;
        this.name = name;
        this.organizer = organizer;
        this.type = type;
    }

    public EventListItemApiDto id(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Get id
     *
     * @return id
     */

    @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EventListItemApiDto date(LocalDate date) {
        this.date = date;
        return this;
    }

    /**
     * Get date
     *
     * @return date
     */
    @NotNull
    @Valid
    @Schema(name = "date", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("date")
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public EventListItemApiDto name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get name
     *
     * @return name
     */
    @NotNull
    @Schema(name = "name", example = "Krátký den", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EventListItemApiDto organizer(String organizer) {
        this.organizer = organizer;
        return this;
    }

    /**
     * Get organizer
     *
     * @return organizer
     */
    @NotNull
    @Schema(name = "organizer", example = "ZBM", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("organizer")
    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public EventListItemApiDto type(TypeEnum type) {
        this.type = type;
        return this;
    }

    /**
     * Get type
     *
     * @return type
     */
    @NotNull
    @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("type")
    public TypeEnum getType() {
        return type;
    }

    public void setType(TypeEnum type) {
        this.type = type;
    }

    public EventListItemApiDto web(String web) {
        this.web = web;
        return this;
    }

    /**
     * Get web
     *
     * @return web
     */

    @Schema(name = "web", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("web")
    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public EventListItemApiDto registrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
        return this;
    }

    /**
     * Get registrationDeadline
     *
     * @return registrationDeadline
     */
    @Valid
    @Schema(name = "registrationDeadline", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("registrationDeadline")
    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public EventListItemApiDto coordinator(String coordinator) {
        this.coordinator = coordinator;
        return this;
    }

    /**
     * Get coordinator
     *
     * @return coordinator
     */

    @Schema(name = "coordinator", example = "Josef Pařízek", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("coordinator")
    public String getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(String coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EventListItemApiDto eventListItem = (EventListItemApiDto) o;
        return Objects.equals(this.id, eventListItem.id) &&
               Objects.equals(this.date, eventListItem.date) &&
               Objects.equals(this.name, eventListItem.name) &&
               Objects.equals(this.organizer, eventListItem.organizer) &&
               Objects.equals(this.type, eventListItem.type) &&
               Objects.equals(this.web, eventListItem.web) &&
               Objects.equals(this.registrationDeadline, eventListItem.registrationDeadline) &&
               Objects.equals(this.coordinator, eventListItem.coordinator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, date, name, organizer, type, web, registrationDeadline, coordinator);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class EventListItemApiDto {\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    date: ").append(toIndentedString(date)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    organizer: ").append(toIndentedString(organizer)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    web: ").append(toIndentedString(web)).append("\n");
        sb.append("    registrationDeadline: ").append(toIndentedString(registrationDeadline)).append("\n");
        sb.append("    coordinator: ").append(toIndentedString(coordinator)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    public static class Builder {

        private EventListItemApiDto instance;

        public Builder() {
            this(new EventListItemApiDto());
        }

        protected Builder(EventListItemApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(EventListItemApiDto value) {
            this.instance.setId(value.id);
            this.instance.setDate(value.date);
            this.instance.setName(value.name);
            this.instance.setOrganizer(value.organizer);
            this.instance.setType(value.type);
            this.instance.setWeb(value.web);
            this.instance.setRegistrationDeadline(value.registrationDeadline);
            this.instance.setCoordinator(value.coordinator);
            return this;
        }

        public Builder id(Integer id) {
            this.instance.id(id);
            return this;
        }

        public Builder date(LocalDate date) {
            this.instance.date(date);
            return this;
        }

        public Builder name(String name) {
            this.instance.name(name);
            return this;
        }

        public Builder organizer(String organizer) {
            this.instance.organizer(organizer);
            return this;
        }

        public Builder type(TypeEnum type) {
            this.instance.type(type);
            return this;
        }

        public Builder web(String web) {
            this.instance.web(web);
            return this;
        }

        public Builder registrationDeadline(LocalDate registrationDeadline) {
            this.instance.registrationDeadline(registrationDeadline);
            return this;
        }

        public Builder coordinator(String coordinator) {
            this.instance.coordinator(coordinator);
            return this;
        }

        /**
         * returns a built EventListItemApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public EventListItemApiDto build() {
            try {
                return this.instance;
            } finally {
                // ensure that this.instance is not reused
                this.instance = null;
            }
        }

        @Override
        public String toString() {
            return getClass() + "=(" + instance + ")";
        }
    }

    /**
     * Create a builder with no initialized field (except for the default values).
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder with a shallow copy of this instance.
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        return builder.copyOf(this);
    }

}

