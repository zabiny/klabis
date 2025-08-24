package club.klabis.users.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import org.springframework.hateoas.RepresentationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data for form setting member grants
 */

@Schema(name = "MemberGrantsForm", description = "Data for form setting member grants")
@JsonTypeName("MemberGrantsForm")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MemberGrantsFormApiDto extends RepresentationModel<MemberGrantsFormApiDto> {

    @Valid
    private List<club.klabis.users.adapters.restapi.dto.GlobalGrantsApiDto> grants = new ArrayList<>();

    public MemberGrantsFormApiDto grants(List<club.klabis.users.adapters.restapi.dto.GlobalGrantsApiDto> grants) {
        this.grants = grants;
        return this;
    }

    public MemberGrantsFormApiDto addGrantsItem(club.klabis.users.adapters.restapi.dto.GlobalGrantsApiDto grantsItem) {
        if (this.grants == null) {
            this.grants = new ArrayList<>();
        }
        this.grants.add(grantsItem);
        return this;
    }

    /**
     * Get grants
     *
     * @return grants
     */
    @Valid
    @Schema(name = "grants", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("grants")
    public List<GlobalGrantsApiDto> getGrants() {
        return grants;
    }

    public void setGrants(List<club.klabis.users.adapters.restapi.dto.GlobalGrantsApiDto> grants) {
        this.grants = grants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MemberGrantsFormApiDto memberGrantsForm = (MemberGrantsFormApiDto) o;
        return Objects.equals(this.grants, memberGrantsForm.grants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grants);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MemberGrantsFormApiDto {\n");
        sb.append("    grants: ").append(toIndentedString(grants)).append("\n");
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

        private MemberGrantsFormApiDto instance;

        public Builder() {
            this(new MemberGrantsFormApiDto());
        }

        protected Builder(MemberGrantsFormApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MemberGrantsFormApiDto value) {
            this.instance.setGrants(value.grants);
            return this;
        }

        public Builder grants(List<club.klabis.users.adapters.restapi.dto.GlobalGrantsApiDto> grants) {
            this.instance.grants(grants);
            return this;
        }

        /**
         * returns a built MemberGrantsFormApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MemberGrantsFormApiDto build() {
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

