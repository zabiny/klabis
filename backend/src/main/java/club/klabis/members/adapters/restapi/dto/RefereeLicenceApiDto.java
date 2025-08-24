package club.klabis.members.adapters.restapi.dto;

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
 * RefereeLicenceApiDto
 */

@JsonTypeName("RefereeLicence")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class RefereeLicenceApiDto extends RepresentationModel<RefereeLicenceApiDto> {

    /**
     * referee license number of the club member
     */
    public enum LicenceEnum {
        R1("R1"),

        R2("R2"),

        R3("R3");

        private String value;

        LicenceEnum(String value) {
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
        public static LicenceEnum fromValue(String value) {
            for (LicenceEnum b : LicenceEnum.values()) {
                if (b.value.equals(value)) {
                    return b;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'");
        }
    }

    private LicenceEnum licence;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    public RefereeLicenceApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public RefereeLicenceApiDto(LicenceEnum licence, LocalDate expiryDate) {
        this.licence = licence;
        this.expiryDate = expiryDate;
    }

    public RefereeLicenceApiDto licence(LicenceEnum licence) {
        this.licence = licence;
        return this;
    }

    /**
     * referee license number of the club member
     *
     * @return licence
     */
    @NotNull
    @Schema(name = "licence", description = "referee license number of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("licence")
    public LicenceEnum getLicence() {
        return licence;
    }

    public void setLicence(LicenceEnum licence) {
        this.licence = licence;
    }

    public RefereeLicenceApiDto expiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    /**
     * Expiry date of the license
     *
     * @return expiryDate
     */
    @NotNull
    @Valid
    @Schema(name = "expiryDate", description = "Expiry date of the license", requiredMode = Schema.RequiredMode.REQUIRED)
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
        RefereeLicenceApiDto refereeLicence = (RefereeLicenceApiDto) o;
        return Objects.equals(this.licence, refereeLicence.licence) &&
               Objects.equals(this.expiryDate, refereeLicence.expiryDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licence, expiryDate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class RefereeLicenceApiDto {\n");
        sb.append("    licence: ").append(toIndentedString(licence)).append("\n");
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

        private RefereeLicenceApiDto instance;

        public Builder() {
            this(new RefereeLicenceApiDto());
        }

        protected Builder(RefereeLicenceApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(RefereeLicenceApiDto value) {
            this.instance.setLicence(value.licence);
            this.instance.setExpiryDate(value.expiryDate);
            return this;
        }

        public Builder licence(LicenceEnum licence) {
            this.instance.licence(licence);
            return this;
        }

        public Builder expiryDate(LocalDate expiryDate) {
            this.instance.expiryDate(expiryDate);
            return this;
        }

        /**
         * returns a built RefereeLicenceApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public RefereeLicenceApiDto build() {
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

