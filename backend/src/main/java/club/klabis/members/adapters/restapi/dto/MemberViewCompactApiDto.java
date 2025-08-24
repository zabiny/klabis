package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * &#39;compact&#39; view of Member
 */

@Schema(name = "MemberViewCompact", description = "'compact' view of Member ")
@JsonTypeName("MemberViewCompact")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MemberViewCompactApiDto extends RepresentationModel<MemberViewCompactApiDto> implements MembersListItemsInnerApiDto {

    private Integer id;

    private String firstName;

    private String lastName;

    private String registrationNumber;

    public MemberViewCompactApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public MemberViewCompactApiDto(Integer id, String firstName, String lastName, String registrationNumber) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.registrationNumber = registrationNumber;
    }

    public MemberViewCompactApiDto id(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Unique identifier for the club member
     *
     * @return id
     */
    @NotNull
    @Schema(name = "id", description = "Unique identifier for the club member", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MemberViewCompactApiDto firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    /**
     * First name of the club member
     *
     * @return firstName
     */
    @NotNull
    @Schema(name = "firstName", description = "First name of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("firstName")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public MemberViewCompactApiDto lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    /**
     * Last name of the club member
     *
     * @return lastName
     */
    @NotNull
    @Schema(name = "lastName", description = "Last name of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("lastName")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public MemberViewCompactApiDto registrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
        return this;
    }

    /**
     * ORIS registration number
     *
     * @return registrationNumber
     */
    @NotNull
    @Pattern(regexp = "^[A-Z]{3}[0-9]{4}$")
    @Schema(name = "registrationNumber", description = "ORIS registration number", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("registrationNumber")
    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MemberViewCompactApiDto memberViewCompact = (MemberViewCompactApiDto) o;
        return Objects.equals(this.id, memberViewCompact.id) &&
               Objects.equals(this.firstName, memberViewCompact.firstName) &&
               Objects.equals(this.lastName, memberViewCompact.lastName) &&
               Objects.equals(this.registrationNumber, memberViewCompact.registrationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, registrationNumber);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MemberViewCompactApiDto {\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
        sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
        sb.append("    registrationNumber: ").append(toIndentedString(registrationNumber)).append("\n");
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

        private MemberViewCompactApiDto instance;

        public Builder() {
            this(new MemberViewCompactApiDto());
        }

        protected Builder(MemberViewCompactApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MemberViewCompactApiDto value) {
            this.instance.setId(value.id);
            this.instance.setFirstName(value.firstName);
            this.instance.setLastName(value.lastName);
            this.instance.setRegistrationNumber(value.registrationNumber);
            return this;
        }

        public Builder id(Integer id) {
            this.instance.id(id);
            return this;
        }

        public Builder firstName(String firstName) {
            this.instance.firstName(firstName);
            return this;
        }

        public Builder lastName(String lastName) {
            this.instance.lastName(lastName);
            return this;
        }

        public Builder registrationNumber(String registrationNumber) {
            this.instance.registrationNumber(registrationNumber);
            return this;
        }

        /**
         * returns a built MemberViewCompactApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MemberViewCompactApiDto build() {
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

