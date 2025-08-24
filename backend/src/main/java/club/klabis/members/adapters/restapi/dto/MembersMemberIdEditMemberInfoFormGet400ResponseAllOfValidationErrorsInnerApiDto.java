package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto
 */

@JsonTypeName("_members__memberId__editMemberInfoForm_get_400_response_allOf_validationErrors_inner")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto extends RepresentationModel<MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto> {

    private String fieldName;

    private String errorMessage;

    public MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto fieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    /**
     * Get fieldName
     *
     * @return fieldName
     */

    @Schema(name = "fieldName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("fieldName")
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    /**
     * Get errorMessage
     *
     * @return errorMessage
     */

    @Schema(name = "errorMessage", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("errorMessage")
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto membersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInner = (MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto) o;
        return Objects.equals(this.fieldName,
                membersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInner.fieldName) &&
               Objects.equals(this.errorMessage,
                       membersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInner.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, errorMessage);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto {\n");
        sb.append("    fieldName: ").append(toIndentedString(fieldName)).append("\n");
        sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
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

        private MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto instance;

        public Builder() {
            this(new MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto());
        }

        protected Builder(MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto value) {
            this.instance.setFieldName(value.fieldName);
            this.instance.setErrorMessage(value.errorMessage);
            return this;
        }

        public Builder fieldName(String fieldName) {
            this.instance.fieldName(fieldName);
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.instance.errorMessage(errorMessage);
            return this;
        }

        /**
         * returns a built MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto build() {
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

