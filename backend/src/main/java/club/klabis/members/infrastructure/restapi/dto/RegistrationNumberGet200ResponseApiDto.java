package club.klabis.members.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * RegistrationNumberGet200ResponseApiDto
 */

@JsonTypeName("_registrationNumber_get_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class RegistrationNumberGet200ResponseApiDto extends RepresentationModel<RegistrationNumberGet200ResponseApiDto> {

    private String suggestedRegistrationNumber;

    public RegistrationNumberGet200ResponseApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public RegistrationNumberGet200ResponseApiDto(String suggestedRegistrationNumber) {
        this.suggestedRegistrationNumber = suggestedRegistrationNumber;
    }

    public RegistrationNumberGet200ResponseApiDto suggestedRegistrationNumber(String suggestedRegistrationNumber) {
        this.suggestedRegistrationNumber = suggestedRegistrationNumber;
        return this;
    }

    /**
     * ORIS registration number
     *
     * @return suggestedRegistrationNumber
     */
    @NotNull
    @Pattern(regexp = "^[A-Z]{3}[0-9]{4}$")
    @Schema(name = "suggestedRegistrationNumber", description = "ORIS registration number", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("suggestedRegistrationNumber")
    public String getSuggestedRegistrationNumber() {
        return suggestedRegistrationNumber;
    }

    public void setSuggestedRegistrationNumber(String suggestedRegistrationNumber) {
        this.suggestedRegistrationNumber = suggestedRegistrationNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RegistrationNumberGet200ResponseApiDto registrationNumberGet200Response = (RegistrationNumberGet200ResponseApiDto) o;
        return Objects.equals(this.suggestedRegistrationNumber,
                registrationNumberGet200Response.suggestedRegistrationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(suggestedRegistrationNumber);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class RegistrationNumberGet200ResponseApiDto {\n");
        sb.append("    suggestedRegistrationNumber: ")
                .append(toIndentedString(suggestedRegistrationNumber))
                .append("\n");
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

        private RegistrationNumberGet200ResponseApiDto instance;

        public Builder() {
            this(new RegistrationNumberGet200ResponseApiDto());
        }

        protected Builder(RegistrationNumberGet200ResponseApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(RegistrationNumberGet200ResponseApiDto value) {
            this.instance.setSuggestedRegistrationNumber(value.suggestedRegistrationNumber);
            return this;
        }

        public Builder suggestedRegistrationNumber(String suggestedRegistrationNumber) {
            this.instance.suggestedRegistrationNumber(suggestedRegistrationNumber);
            return this;
        }

        /**
         * returns a built RegistrationNumberGet200ResponseApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public RegistrationNumberGet200ResponseApiDto build() {
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

