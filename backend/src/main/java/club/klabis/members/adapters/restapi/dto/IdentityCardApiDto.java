package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDate;
import java.util.Objects;

/**
 * IdentityCardApiDto
 */

@JsonTypeName("IdentityCard")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class IdentityCardApiDto extends RepresentationModel<IdentityCardApiDto> {

    private String number;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    public IdentityCardApiDto number(String number) {
        this.number = number;
        return this;
    }

    /**
     * Personal identification number of the club member
     *
     * @return number
     */

    @Schema(name = "number", description = "Personal identification number of the club member", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("number")
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public IdentityCardApiDto expiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    /**
     * Expiry date of the ID card, YYYY-MM-DD
     *
     * @return expiryDate
     */
    @Valid
    @Schema(name = "expiryDate", description = "Expiry date of the ID card, YYYY-MM-DD", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("expiryDate")
    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IdentityCardApiDto identityCard = (IdentityCardApiDto) o;
        return Objects.equals(this.number, identityCard.number) &&
               Objects.equals(this.expiryDate, identityCard.expiryDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, expiryDate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class IdentityCardApiDto {\n");
        sb.append("    number: ").append(toIndentedString(number)).append("\n");
        sb.append("    expiryDate: ").append(toIndentedString(expiryDate)).append("\n");
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

        private IdentityCardApiDto instance;

        public Builder() {
            this(new IdentityCardApiDto());
        }

        protected Builder(IdentityCardApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(IdentityCardApiDto value) {
            this.instance.setNumber(value.number);
            this.instance.setExpiryDate(value.expiryDate);
            return this;
        }

        public Builder number(String number) {
            this.instance.number(number);
            return this;
        }

        public Builder expiryDate(LocalDate expiryDate) {
            this.instance.expiryDate(expiryDate);
            return this;
        }

        /**
         * returns a built IdentityCardApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public IdentityCardApiDto build() {
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

