package club.klabis.oris.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * User data retrieved from ORIS    #### Required authorization - requires &#x60;members:register&#x60; grant
 */

@Schema(name = "ORISUserInfo", description = "User data retrieved from ORIS    #### Required authorization - requires `members:register` grant")
@JsonTypeName("ORISUserInfo")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class ORISUserInfoApiDto extends RepresentationModel<ORISUserInfoApiDto> {

    private String firstName;

    private String lastName;

    private String registrationNumber;

    private Integer orisId;

    public ORISUserInfoApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public ORISUserInfoApiDto(String firstName, String lastName, String registrationNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.registrationNumber = registrationNumber;
    }

    public ORISUserInfoApiDto firstName(String firstName) {
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

    public ORISUserInfoApiDto lastName(String lastName) {
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

    public ORISUserInfoApiDto registrationNumber(String registrationNumber) {
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

    public ORISUserInfoApiDto orisId(Integer orisId) {
        this.orisId = orisId;
        return this;
    }

    /**
     * Oris ID of registered orienteering runner
     *
     * @return orisId
     */

    @Schema(name = "orisId", description = "Oris ID of registered orienteering runner", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("orisId")
    public Integer getOrisId() {
        return orisId;
    }

    public void setOrisId(Integer orisId) {
        this.orisId = orisId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ORISUserInfoApiDto orISUserInfo = (ORISUserInfoApiDto) o;
        return Objects.equals(this.firstName, orISUserInfo.firstName) &&
               Objects.equals(this.lastName, orISUserInfo.lastName) &&
               Objects.equals(this.registrationNumber, orISUserInfo.registrationNumber) &&
               Objects.equals(this.orisId, orISUserInfo.orisId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, registrationNumber, orisId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ORISUserInfoApiDto {\n");
        sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
        sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
        sb.append("    registrationNumber: ").append(toIndentedString(registrationNumber)).append("\n");
        sb.append("    orisId: ").append(toIndentedString(orisId)).append("\n");
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

        private ORISUserInfoApiDto instance;

        public Builder() {
            this(new ORISUserInfoApiDto());
        }

        protected Builder(ORISUserInfoApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(ORISUserInfoApiDto value) {
            this.instance.setFirstName(value.firstName);
            this.instance.setLastName(value.lastName);
            this.instance.setRegistrationNumber(value.registrationNumber);
            this.instance.setOrisId(value.orisId);
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

        public Builder orisId(Integer orisId) {
            this.instance.orisId(orisId);
            return this;
        }

        /**
         * returns a built ORISUserInfoApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public ORISUserInfoApiDto build() {
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

