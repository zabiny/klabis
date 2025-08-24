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
 * AddressApiDto
 */

@JsonTypeName("Address")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class AddressApiDto extends RepresentationModel<AddressApiDto> {

    private String streetAndNumber;

    private String city;

    private String postalCode;

    private String country;

    public AddressApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public AddressApiDto(String streetAndNumber, String city, String postalCode, String country) {
        this.streetAndNumber = streetAndNumber;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    public AddressApiDto streetAndNumber(String streetAndNumber) {
        this.streetAndNumber = streetAndNumber;
        return this;
    }

    /**
     * Street name and number
     *
     * @return streetAndNumber
     */
    @NotNull
    @Schema(name = "streetAndNumber", description = "Street name and number", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("streetAndNumber")
    public String getStreetAndNumber() {
        return streetAndNumber;
    }

    public void setStreetAndNumber(String streetAndNumber) {
        this.streetAndNumber = streetAndNumber;
    }

    public AddressApiDto city(String city) {
        this.city = city;
        return this;
    }

    /**
     * City
     *
     * @return city
     */
    @NotNull
    @Schema(name = "city", description = "City", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("city")
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public AddressApiDto postalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    /**
     * Postal or ZIP code
     *
     * @return postalCode
     */
    @NotNull
    @Schema(name = "postalCode", description = "Postal or ZIP code", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("postalCode")
    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public AddressApiDto country(String country) {
        this.country = country;
        return this;
    }

    /**
     * two letter country code, ISO 3166-1 alpha-2
     *
     * @return country
     */
    @NotNull
    @Pattern(regexp = "^[A-Z]{2}$")
    @Schema(name = "country", description = "two letter country code, ISO 3166-1 alpha-2", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("country")
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AddressApiDto address = (AddressApiDto) o;
        return Objects.equals(this.streetAndNumber, address.streetAndNumber) &&
               Objects.equals(this.city, address.city) &&
               Objects.equals(this.postalCode, address.postalCode) &&
               Objects.equals(this.country, address.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(streetAndNumber, city, postalCode, country);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class AddressApiDto {\n");
        sb.append("    streetAndNumber: ").append(toIndentedString(streetAndNumber)).append("\n");
        sb.append("    city: ").append(toIndentedString(city)).append("\n");
        sb.append("    postalCode: ").append(toIndentedString(postalCode)).append("\n");
        sb.append("    country: ").append(toIndentedString(country)).append("\n");
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

        private AddressApiDto instance;

        public Builder() {
            this(new AddressApiDto());
        }

        protected Builder(AddressApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(AddressApiDto value) {
            this.instance.setStreetAndNumber(value.streetAndNumber);
            this.instance.setCity(value.city);
            this.instance.setPostalCode(value.postalCode);
            this.instance.setCountry(value.country);
            return this;
        }

        public Builder streetAndNumber(String streetAndNumber) {
            this.instance.streetAndNumber(streetAndNumber);
            return this;
        }

        public Builder city(String city) {
            this.instance.city(city);
            return this;
        }

        public Builder postalCode(String postalCode) {
            this.instance.postalCode(postalCode);
            return this;
        }

        public Builder country(String country) {
            this.instance.country(country);
            return this;
        }

        /**
         * returns a built AddressApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public AddressApiDto build() {
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

