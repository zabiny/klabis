package club.klabis.groups.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * MembersGroupListItemApiDto
 */

@JsonTypeName("MembersGroupListItem")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MembersGroupListItemApiDto extends RepresentationModel<MembersGroupListItemApiDto> {

    private BigDecimal id;

    private String name;

    private String description;

    private String address;

    public MembersGroupListItemApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public MembersGroupListItemApiDto(BigDecimal id, String name) {
        this.id = id;
        this.name = name;
    }

    public MembersGroupListItemApiDto id(BigDecimal id) {
        this.id = id;
        return this;
    }

    /**
     * Get id
     *
     * @return id
     */
    @Valid
    @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    public BigDecimal getId() {
        return id;
    }

    public void setId(BigDecimal id) {
        this.id = id;
    }

    public MembersGroupListItemApiDto name(String name) {
        this.name = name;
        return this;
    }

    /**
     * unique name of the group
     *
     * @return name
     */
    @NotNull
    @Schema(name = "name", example = "Trenink - dorost", description = "unique name of the group", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public MembersGroupListItemApiDto description(String description) {
        this.description = description;
        return this;
    }

    /**
     * description of the group allowing to let members know what is this group about, etc..
     *
     * @return description
     */

    @Schema(name = "description", example = "Treninkova skupina Dorost", description = "description of the group allowing to let members know what is this group about, etc..", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MembersGroupListItemApiDto address(String address) {
        this.address = address;
        return this;
    }

    /**
     * Email address of the group
     *
     * @return address
     */
    @Email
    @Schema(name = "address", example = "dorost@zabiny.club", description = "Email address of the group", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("address")
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MembersGroupListItemApiDto membersGroupListItem = (MembersGroupListItemApiDto) o;
        return Objects.equals(this.id, membersGroupListItem.id) &&
               Objects.equals(this.name, membersGroupListItem.name) &&
               Objects.equals(this.description, membersGroupListItem.description) &&
               Objects.equals(this.address, membersGroupListItem.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, address);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MembersGroupListItemApiDto {\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    address: ").append(toIndentedString(address)).append("\n");
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

        private MembersGroupListItemApiDto instance;

        public Builder() {
            this(new MembersGroupListItemApiDto());
        }

        protected Builder(MembersGroupListItemApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MembersGroupListItemApiDto value) {
            this.instance.setId(value.id);
            this.instance.setName(value.name);
            this.instance.setDescription(value.description);
            this.instance.setAddress(value.address);
            return this;
        }

        public Builder id(BigDecimal id) {
            this.instance.id(id);
            return this;
        }

        public Builder name(String name) {
            this.instance.name(name);
            return this;
        }

        public Builder description(String description) {
            this.instance.description(description);
            return this;
        }

        public Builder address(String address) {
            this.instance.address(address);
            return this;
        }

        /**
         * returns a built MembersGroupListItemApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MembersGroupListItemApiDto build() {
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

