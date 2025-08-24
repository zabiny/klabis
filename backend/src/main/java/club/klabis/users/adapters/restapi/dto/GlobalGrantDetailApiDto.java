package club.klabis.users.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * GlobalGrantDetailApiDto
 */

@JsonTypeName("GlobalGrantDetail")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class GlobalGrantDetailApiDto extends RepresentationModel<GlobalGrantDetailApiDto> {

    private GlobalGrantsApiDto grant;

    private String description;

    public GlobalGrantDetailApiDto grant(GlobalGrantsApiDto grant) {
        this.grant = grant;
        return this;
    }

    /**
     * Get grant
     *
     * @return grant
     */
    @Valid
    @Schema(name = "grant", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("grant")
    public GlobalGrantsApiDto getGrant() {
        return grant;
    }

    public void setGrant(GlobalGrantsApiDto grant) {
        this.grant = grant;
    }

    public GlobalGrantDetailApiDto description(String description) {
        this.description = description;
        return this;
    }

    /**
     * User friendly description of the grant
     *
     * @return description
     */

    @Schema(name = "description", description = "User friendly description of the grant", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GlobalGrantDetailApiDto globalGrantDetail = (GlobalGrantDetailApiDto) o;
        return Objects.equals(this.grant, globalGrantDetail.grant) &&
               Objects.equals(this.description, globalGrantDetail.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grant, description);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class GlobalGrantDetailApiDto {\n");
        sb.append("    grant: ").append(toIndentedString(grant)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

        private GlobalGrantDetailApiDto instance;

        public Builder() {
            this(new GlobalGrantDetailApiDto());
        }

        protected Builder(GlobalGrantDetailApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(GlobalGrantDetailApiDto value) {
            this.instance.setGrant(value.grant);
            this.instance.setDescription(value.description);
            return this;
        }

        public Builder grant(GlobalGrantsApiDto grant) {
            this.instance.grant(grant);
            return this;
        }

        public Builder description(String description) {
            this.instance.description(description);
            return this;
        }

        /**
         * returns a built GlobalGrantDetailApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public GlobalGrantDetailApiDto build() {
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

