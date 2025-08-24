package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * LicencesApiDto
 */

@JsonTypeName("Licences")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class LicencesApiDto extends RepresentationModel<LicencesApiDto> {

    private OBLicenceApiDto ob;

    private RefereeLicenceApiDto referee;

    private TrainerLicenceApiDto trainer;

    public LicencesApiDto ob(OBLicenceApiDto ob) {
        this.ob = ob;
        return this;
    }

    /**
     * Get ob
     *
     * @return ob
     */
    @Valid
    @Schema(name = "ob", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("ob")
    public OBLicenceApiDto getOb() {
        return ob;
    }

    public void setOb(OBLicenceApiDto ob) {
        this.ob = ob;
    }

    public LicencesApiDto referee(RefereeLicenceApiDto referee) {
        this.referee = referee;
        return this;
    }

    /**
     * Get referee
     *
     * @return referee
     */
    @Valid
    @Schema(name = "referee", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("referee")
    public RefereeLicenceApiDto getReferee() {
        return referee;
    }

    public void setReferee(RefereeLicenceApiDto referee) {
        this.referee = referee;
    }

    public LicencesApiDto trainer(TrainerLicenceApiDto trainer) {
        this.trainer = trainer;
        return this;
    }

    /**
     * Get trainer
     *
     * @return trainer
     */
    @Valid
    @Schema(name = "trainer", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("trainer")
    public TrainerLicenceApiDto getTrainer() {
        return trainer;
    }

    public void setTrainer(TrainerLicenceApiDto trainer) {
        this.trainer = trainer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LicencesApiDto licences = (LicencesApiDto) o;
        return Objects.equals(this.ob, licences.ob) &&
               Objects.equals(this.referee, licences.referee) &&
               Objects.equals(this.trainer, licences.trainer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ob, referee, trainer);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class LicencesApiDto {\n");
        sb.append("    ob: ").append(toIndentedString(ob)).append("\n");
        sb.append("    referee: ").append(toIndentedString(referee)).append("\n");
        sb.append("    trainer: ").append(toIndentedString(trainer)).append("\n");
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

        private LicencesApiDto instance;

        public Builder() {
            this(new LicencesApiDto());
        }

        protected Builder(LicencesApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(LicencesApiDto value) {
            this.instance.setOb(value.ob);
            this.instance.setReferee(value.referee);
            this.instance.setTrainer(value.trainer);
            return this;
        }

        public Builder ob(OBLicenceApiDto ob) {
            this.instance.ob(ob);
            return this;
        }

        public Builder referee(RefereeLicenceApiDto referee) {
            this.instance.referee(referee);
            return this;
        }

        public Builder trainer(TrainerLicenceApiDto trainer) {
            this.instance.trainer(trainer);
            return this;
        }

        /**
         * returns a built LicencesApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public LicencesApiDto build() {
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

