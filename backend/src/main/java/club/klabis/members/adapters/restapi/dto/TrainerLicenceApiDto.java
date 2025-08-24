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
 * TrainerLicenceApiDto
 */

@JsonTypeName("TrainerLicence")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class TrainerLicenceApiDto extends RepresentationModel<TrainerLicenceApiDto> {

    /**
     * trainer license number of the club member
     */
    public enum LicenceEnum {
        T1("T1"),

        T2("T2"),

        T3("T3");

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

    public TrainerLicenceApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public TrainerLicenceApiDto(LicenceEnum licence, LocalDate expiryDate) {
        this.licence = licence;
        this.expiryDate = expiryDate;
    }

    public TrainerLicenceApiDto licence(LicenceEnum licence) {
        this.licence = licence;
        return this;
    }

    /**
     * trainer license number of the club member
     *
     * @return licence
     */
    @NotNull
    @Schema(name = "licence", description = "trainer license number of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("licence")
    public LicenceEnum getLicence() {
        return licence;
    }

    public void setLicence(LicenceEnum licence) {
        this.licence = licence;
    }

    public TrainerLicenceApiDto expiryDate(LocalDate expiryDate) {
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
        TrainerLicenceApiDto trainerLicence = (TrainerLicenceApiDto) o;
        return Objects.equals(this.licence, trainerLicence.licence) &&
               Objects.equals(this.expiryDate, trainerLicence.expiryDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licence, expiryDate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class TrainerLicenceApiDto {\n");
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

        private TrainerLicenceApiDto instance;

        public Builder() {
            this(new TrainerLicenceApiDto());
        }

        protected Builder(TrainerLicenceApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(TrainerLicenceApiDto value) {
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
         * returns a built TrainerLicenceApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public TrainerLicenceApiDto build() {
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

