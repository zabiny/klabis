package club.klabis.events.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import org.springframework.hateoas.RepresentationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GetEvents200ResponseApiDto
 */

@JsonTypeName("getEvents_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class GetEvents200ResponseApiDto extends RepresentationModel<GetEvents200ResponseApiDto> {

    @Valid
    private List<@Valid EventListItemApiDto> items = new ArrayList<>();

    public GetEvents200ResponseApiDto items(List<@Valid EventListItemApiDto> items) {
        this.items = items;
        return this;
    }

    public GetEvents200ResponseApiDto addItemsItem(EventListItemApiDto itemsItem) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(itemsItem);
        return this;
    }

    /**
     * Get items
     *
     * @return items
     */
    @Valid
    @Schema(name = "items", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("items")
    public List<@Valid EventListItemApiDto> getItems() {
        return items;
    }

    public void setItems(List<@Valid EventListItemApiDto> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GetEvents200ResponseApiDto getEvents200Response = (GetEvents200ResponseApiDto) o;
        return Objects.equals(this.items, getEvents200Response.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class GetEvents200ResponseApiDto {\n");
        sb.append("    items: ").append(toIndentedString(items)).append("\n");
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

        private GetEvents200ResponseApiDto instance;

        public Builder() {
            this(new GetEvents200ResponseApiDto());
        }

        protected Builder(GetEvents200ResponseApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(GetEvents200ResponseApiDto value) {
            this.instance.setItems(value.items);
            return this;
        }

        public Builder items(List<@Valid EventListItemApiDto> items) {
            this.instance.items(items);
            return this;
        }

        /**
         * returns a built GetEvents200ResponseApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public GetEvents200ResponseApiDto build() {
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

