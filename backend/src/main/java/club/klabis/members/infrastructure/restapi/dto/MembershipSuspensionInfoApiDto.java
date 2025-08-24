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
 * MembershipSuspensionInfoApiDto
 */

@JsonTypeName("MembershipSuspensionInfo")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MembershipSuspensionInfoApiDto extends RepresentationModel<MembershipSuspensionInfoApiDto> {

    private Boolean isSuspended;

    private Boolean canSuspend;

    private SuspendMembershipBlockersApiDto details;

    public MembershipSuspensionInfoApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public MembershipSuspensionInfoApiDto(Boolean isSuspended, Boolean canSuspend, SuspendMembershipBlockersApiDto details) {
        this.isSuspended = isSuspended;
        this.canSuspend = canSuspend;
        this.details = details;
    }

    public MembershipSuspensionInfoApiDto isSuspended(Boolean isSuspended) {
        this.isSuspended = isSuspended;
        return this;
    }

    /**
     * tells if member account is currently suspended
     *
     * @return isSuspended
     */
    @NotNull
    @Schema(name = "isSuspended", description = "tells if member account is currently suspended", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("isSuspended")
    public Boolean isIsSuspended() {
        return isSuspended;
    }

    public void setIsSuspended(Boolean isSuspended) {
        this.isSuspended = isSuspended;
    }

    public MembershipSuspensionInfoApiDto canSuspend(Boolean canSuspend) {
        this.canSuspend = canSuspend;
        return this;
    }

    /**
     * tells if member account can be suspended
     *
     * @return canSuspend
     */
    @NotNull
    @Schema(name = "canSuspend", description = "tells if member account can be suspended", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("canSuspend")
    public Boolean isCanSuspend() {
        return canSuspend;
    }

    public void setCanSuspend(Boolean canSuspend) {
        this.canSuspend = canSuspend;
    }

    public MembershipSuspensionInfoApiDto details(SuspendMembershipBlockersApiDto details) {
        this.details = details;
        return this;
    }

    /**
     * Get details
     *
     * @return details
     */
    @NotNull
    @Valid
    @Schema(name = "details", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("details")
    public SuspendMembershipBlockersApiDto getDetails() {
        return details;
    }

    public void setDetails(SuspendMembershipBlockersApiDto details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MembershipSuspensionInfoApiDto membershipSuspensionInfo = (MembershipSuspensionInfoApiDto) o;
        return Objects.equals(this.isSuspended, membershipSuspensionInfo.isSuspended) &&
               Objects.equals(this.canSuspend, membershipSuspensionInfo.canSuspend) &&
               Objects.equals(this.details, membershipSuspensionInfo.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSuspended, canSuspend, details);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MembershipSuspensionInfoApiDto {\n");
        sb.append("    isSuspended: ").append(toIndentedString(isSuspended)).append("\n");
        sb.append("    canSuspend: ").append(toIndentedString(canSuspend)).append("\n");
        sb.append("    details: ").append(toIndentedString(details)).append("\n");
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

        private MembershipSuspensionInfoApiDto instance;

        public Builder() {
            this(new MembershipSuspensionInfoApiDto());
        }

        protected Builder(MembershipSuspensionInfoApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MembershipSuspensionInfoApiDto value) {
            this.instance.setIsSuspended(value.isSuspended);
            this.instance.setCanSuspend(value.canSuspend);
            this.instance.setDetails(value.details);
            return this;
        }

        public Builder isSuspended(Boolean isSuspended) {
            this.instance.isSuspended(isSuspended);
            return this;
        }

        public Builder canSuspend(Boolean canSuspend) {
            this.instance.canSuspend(canSuspend);
            return this;
        }

        public Builder details(SuspendMembershipBlockersApiDto details) {
            this.instance.details(details);
            return this;
        }

        /**
         * returns a built MembershipSuspensionInfoApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MembershipSuspensionInfoApiDto build() {
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

