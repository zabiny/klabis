package club.klabis.members.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * SuspendMembershipBlockersFinanceApiDto
 */

@JsonTypeName("SuspendMembershipBlockers_finance")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class SuspendMembershipBlockersFinanceApiDto extends RepresentationModel<SuspendMembershipBlockersFinanceApiDto> {

    private Boolean status;

    public SuspendMembershipBlockersFinanceApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public SuspendMembershipBlockersFinanceApiDto(Boolean status) {
        this.status = status;
    }

    public SuspendMembershipBlockersFinanceApiDto status(Boolean status) {
        this.status = status;
        return this;
    }

    /**
     * tells if finance account balance permits membership suspension
     *
     * @return status
     */
    @NotNull
    @Schema(name = "status", description = "tells if finance account balance permits membership suspension", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("status")
    public Boolean isStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SuspendMembershipBlockersFinanceApiDto suspendMembershipBlockersFinance = (SuspendMembershipBlockersFinanceApiDto) o;
        return Objects.equals(this.status, suspendMembershipBlockersFinance.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SuspendMembershipBlockersFinanceApiDto {\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

        private SuspendMembershipBlockersFinanceApiDto instance;

        public Builder() {
            this(new SuspendMembershipBlockersFinanceApiDto());
        }

        protected Builder(SuspendMembershipBlockersFinanceApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(SuspendMembershipBlockersFinanceApiDto value) {
            this.instance.setStatus(value.status);
            return this;
        }

        public Builder status(Boolean status) {
            this.instance.status(status);
            return this;
        }

        /**
         * returns a built SuspendMembershipBlockersFinanceApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public SuspendMembershipBlockersFinanceApiDto build() {
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

