package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * OBLicenceApiDto
 */

@JsonTypeName("OBLicence")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class OBLicenceApiDto extends RepresentationModel<OBLicenceApiDto> {

    /**
     * License number of the club member
     */
    public enum LicenceEnum {
        E("E"),

        R("R"),

        A("A"),

        B("B"),

        C("C");

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

    public OBLicenceApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public OBLicenceApiDto(LicenceEnum licence) {
        this.licence = licence;
    }

    public OBLicenceApiDto licence(LicenceEnum licence) {
        this.licence = licence;
        return this;
    }

    /**
     * License number of the club member
     *
     * @return licence
     */
    @NotNull
    @Schema(name = "licence", description = "License number of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("licence")
    public LicenceEnum getLicence() {
        return licence;
    }

    public void setLicence(LicenceEnum licence) {
        this.licence = licence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OBLicenceApiDto obLicence = (OBLicenceApiDto) o;
        return Objects.equals(this.licence, obLicence.licence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licence);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OBLicenceApiDto {\n");
        sb.append("    licence: ").append(toIndentedString(licence)).append("\n");
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

        private OBLicenceApiDto instance;

        public Builder() {
            this(new OBLicenceApiDto());
        }

        protected Builder(OBLicenceApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(OBLicenceApiDto value) {
            this.instance.setLicence(value.licence);
            return this;
        }

        public Builder licence(LicenceEnum licence) {
            this.instance.licence(licence);
            return this;
        }

        /**
         * returns a built OBLicenceApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public OBLicenceApiDto build() {
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

