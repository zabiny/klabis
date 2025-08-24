package club.klabis.groups.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * MembersGroupListApiDto
 */

@JsonTypeName("MembersGroupList")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MembersGroupListApiDto extends RepresentationModel<MembersGroupListApiDto> {

    private MembersGroupListItemApiDto items;

    public MembersGroupListApiDto items(MembersGroupListItemApiDto items) {
        this.items = items;
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
    public MembersGroupListItemApiDto getItems() {
        return items;
    }

    public void setItems(MembersGroupListItemApiDto items) {
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
        MembersGroupListApiDto membersGroupList = (MembersGroupListApiDto) o;
        return Objects.equals(this.items, membersGroupList.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MembersGroupListApiDto {\n");
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

        private MembersGroupListApiDto instance;

        public Builder() {
            this(new MembersGroupListApiDto());
        }

        protected Builder(MembersGroupListApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MembersGroupListApiDto value) {
            this.instance.setItems(value.items);
            return this;
        }

        public Builder items(MembersGroupListItemApiDto items) {
            this.instance.items(items);
            return this;
        }

        /**
         * returns a built MembersGroupListApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MembersGroupListApiDto build() {
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

