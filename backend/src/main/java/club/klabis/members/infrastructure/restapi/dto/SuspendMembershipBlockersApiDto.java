package club.klabis.members.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * describes conditions which may prevent membership suspension and their actual status
 */

@Schema(name = "SuspendMembershipBlockers", description = "describes conditions which may prevent membership suspension and their actual status")
@JsonTypeName("SuspendMembershipBlockers")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class SuspendMembershipBlockersApiDto extends RepresentationModel<SuspendMembershipBlockersApiDto> {

    private SuspendMembershipBlockersFinanceApiDto finance;

    public SuspendMembershipBlockersApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public SuspendMembershipBlockersApiDto(SuspendMembershipBlockersFinanceApiDto finance) {
        this.finance = finance;
    }

    public SuspendMembershipBlockersApiDto finance(SuspendMembershipBlockersFinanceApiDto finance) {
        this.finance = finance;
        return this;
    }

    /**
     * Get finance
     *
     * @return finance
     */
    @NotNull
    @Valid
    @Schema(name = "finance", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("finance")
    public SuspendMembershipBlockersFinanceApiDto getFinance() {
        return finance;
    }

    public void setFinance(SuspendMembershipBlockersFinanceApiDto finance) {
        this.finance = finance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SuspendMembershipBlockersApiDto suspendMembershipBlockers = (SuspendMembershipBlockersApiDto) o;
        return Objects.equals(this.finance, suspendMembershipBlockers.finance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(finance);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SuspendMembershipBlockersApiDto {\n");
        sb.append("    finance: ").append(toIndentedString(finance)).append("\n");
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

        private SuspendMembershipBlockersApiDto instance;

        public Builder() {
            this(new SuspendMembershipBlockersApiDto());
        }

        protected Builder(SuspendMembershipBlockersApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(SuspendMembershipBlockersApiDto value) {
            this.instance.setFinance(value.finance);
            return this;
        }

        public Builder finance(SuspendMembershipBlockersFinanceApiDto finance) {
            this.instance.finance(finance);
            return this;
        }

        /**
         * returns a built SuspendMembershipBlockersApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public SuspendMembershipBlockersApiDto build() {
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

