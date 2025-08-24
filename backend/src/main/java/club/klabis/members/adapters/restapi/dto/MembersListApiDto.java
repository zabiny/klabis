package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * List of members.
 */

@Schema(name = "MembersList", description = "List of members.")
@JsonTypeName("MembersList")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MembersListApiDto extends RepresentationModel<MembersListApiDto> {

    @Valid
    private List<MembersListItemsInnerApiDto> items = new ArrayList<>();

    public MembersListApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public MembersListApiDto(List<MembersListItemsInnerApiDto> items) {
        this.items = items;
    }

    public MembersListApiDto items(List<MembersListItemsInnerApiDto> items) {
        this.items = items;
        return this;
    }

    public MembersListApiDto addItemsItem(MembersListItemsInnerApiDto itemsItem) {
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
    @NotNull
    @Valid
    @Schema(name = "items", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("items")
    public List<MembersListItemsInnerApiDto> getItems() {
        return items;
    }

    public void setItems(List<MembersListItemsInnerApiDto> items) {
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
        MembersListApiDto membersList = (MembersListApiDto) o;
        return Objects.equals(this.items, membersList.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MembersListApiDto {\n");
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

        private MembersListApiDto instance;

        public Builder() {
            this(new MembersListApiDto());
        }

        protected Builder(MembersListApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MembersListApiDto value) {
            this.instance.setItems(value.items);
            return this;
        }

        public Builder items(List<MembersListItemsInnerApiDto> items) {
            this.instance.items(items);
            return this;
        }

        /**
         * returns a built MembersListApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MembersListApiDto build() {
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

