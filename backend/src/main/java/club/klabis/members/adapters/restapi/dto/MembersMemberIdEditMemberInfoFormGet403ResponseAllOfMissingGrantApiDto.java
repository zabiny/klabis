package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.annotation.Generated;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * MembersMemberIdEditMemberInfoFormGet403ResponseAllOfMissingGrantApiDto
 */

@JsonTypeName("_members__memberId__editMemberInfoForm_get_403_response_allOf_missingGrant")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MembersMemberIdEditMemberInfoFormGet403ResponseAllOfMissingGrantApiDto extends RepresentationModel<MembersMemberIdEditMemberInfoFormGet403ResponseAllOfMissingGrantApiDto> {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MembersMemberIdEditMemberInfoFormGet403ResponseAllOfMissingGrantApiDto {\n");
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

        private MembersMemberIdEditMemberInfoFormGet403ResponseAllOfMissingGrantApiDto instance;

        public Builder() {
            this(new MembersMemberIdEditMemberInfoFormGet403ResponseAllOfMissingGrantApiDto());
        }

        protected Builder(MembersMemberIdEditMemberInfoFormGet403ResponseAllOfMissingGrantApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MembersMemberIdEditMemberInfoFormGet403ResponseAllOfMissingGrantApiDto value) {
            return this;
        }

        /**
         * returns a built MembersMemberIdEditMemberInfoFormGet403ResponseAllOfMissingGrantApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MembersMemberIdEditMemberInfoFormGet403ResponseAllOfMissingGrantApiDto build() {
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

