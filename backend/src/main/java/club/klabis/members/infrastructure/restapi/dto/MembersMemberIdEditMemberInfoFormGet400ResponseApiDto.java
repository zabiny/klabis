package club.klabis.members.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MembersMemberIdEditMemberInfoFormGet400ResponseApiDto
 */

@JsonTypeName("_members__memberId__editMemberInfoForm_get_400_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MembersMemberIdEditMemberInfoFormGet400ResponseApiDto extends RepresentationModel<MembersMemberIdEditMemberInfoFormGet400ResponseApiDto> {

    private String title;

    private Integer status;

    private String detail;

    private String instance;

    private String type;

    @Valid
    private List<@Valid MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto> validationErrors = new ArrayList<>();

    public MembersMemberIdEditMemberInfoFormGet400ResponseApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public MembersMemberIdEditMemberInfoFormGet400ResponseApiDto(String title, Integer status, String detail, String instance) {
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
    }

    public MembersMemberIdEditMemberInfoFormGet400ResponseApiDto title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Description of the error status
     *
     * @return title
     */
    @NotNull
    @Schema(name = "title", description = "Description of the error status", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MembersMemberIdEditMemberInfoFormGet400ResponseApiDto status(Integer status) {
        this.status = status;
        return this;
    }

    /**
     * error status value
     *
     * @return status
     */
    @NotNull
    @Schema(name = "status", description = "error status value", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("status")
    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public MembersMemberIdEditMemberInfoFormGet400ResponseApiDto detail(String detail) {
        this.detail = detail;
        return this;
    }

    /**
     * User friendly description of the error
     *
     * @return relatedItem
     */
    @NotNull
    @Schema(name = "relatedItem", description = "User friendly description of the error", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("relatedItem")
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public MembersMemberIdEditMemberInfoFormGet400ResponseApiDto instance(String instance) {
        this.instance = instance;
        return this;
    }

    /**
     * URI of the resource which has thrown the error
     *
     * @return instance
     */
    @NotNull
    @Schema(name = "instance", description = "URI of the resource which has thrown the error", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("instance")
    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public MembersMemberIdEditMemberInfoFormGet400ResponseApiDto type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Get type
     *
     * @return type
     */

    @Schema(name = "type", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public MembersMemberIdEditMemberInfoFormGet400ResponseApiDto validationErrors(List<@Valid MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto> validationErrors) {
        this.validationErrors = validationErrors;
        return this;
    }

    public MembersMemberIdEditMemberInfoFormGet400ResponseApiDto addValidationErrorsItem(MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto validationErrorsItem) {
        if (this.validationErrors == null) {
            this.validationErrors = new ArrayList<>();
        }
        this.validationErrors.add(validationErrorsItem);
        return this;
    }

    /**
     * Get validationErrors
     *
     * @return validationErrors
     */
    @Valid
    @Schema(name = "validationErrors", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("validationErrors")
    public List<@Valid MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<@Valid MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto> validationErrors) {
        this.validationErrors = validationErrors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MembersMemberIdEditMemberInfoFormGet400ResponseApiDto membersMemberIdEditMemberInfoFormGet400Response = (MembersMemberIdEditMemberInfoFormGet400ResponseApiDto) o;
        return Objects.equals(this.title, membersMemberIdEditMemberInfoFormGet400Response.title) &&
               Objects.equals(this.status, membersMemberIdEditMemberInfoFormGet400Response.status) &&
               Objects.equals(this.detail, membersMemberIdEditMemberInfoFormGet400Response.detail) &&
               Objects.equals(this.instance, membersMemberIdEditMemberInfoFormGet400Response.instance) &&
               Objects.equals(this.type, membersMemberIdEditMemberInfoFormGet400Response.type) &&
               Objects.equals(this.validationErrors, membersMemberIdEditMemberInfoFormGet400Response.validationErrors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, status, detail, instance, type, validationErrors);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MembersMemberIdEditMemberInfoFormGet400ResponseApiDto {\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    relatedItem: ").append(toIndentedString(detail)).append("\n");
        sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    validationErrors: ").append(toIndentedString(validationErrors)).append("\n");
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

        private MembersMemberIdEditMemberInfoFormGet400ResponseApiDto instance;

        public Builder() {
            this(new MembersMemberIdEditMemberInfoFormGet400ResponseApiDto());
        }

        protected Builder(MembersMemberIdEditMemberInfoFormGet400ResponseApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MembersMemberIdEditMemberInfoFormGet400ResponseApiDto value) {
            this.instance.setTitle(value.title);
            this.instance.setStatus(value.status);
            this.instance.setDetail(value.detail);
            this.instance.setInstance(value.instance);
            this.instance.setType(value.type);
            this.instance.setValidationErrors(value.validationErrors);
            return this;
        }

        public Builder title(String title) {
            this.instance.title(title);
            return this;
        }

        public Builder status(Integer status) {
            this.instance.status(status);
            return this;
        }

        public Builder detail(String detail) {
            this.instance.detail(detail);
            return this;
        }

        public Builder instance(String instance) {
            this.instance.instance(instance);
            return this;
        }

        public Builder type(String type) {
            this.instance.type(type);
            return this;
        }

        public Builder validationErrors(List<@Valid MembersMemberIdEditMemberInfoFormGet400ResponseAllOfValidationErrorsInnerApiDto> validationErrors) {
            this.instance.validationErrors(validationErrors);
            return this;
        }

        /**
         * returns a built MembersMemberIdEditMemberInfoFormGet400ResponseApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MembersMemberIdEditMemberInfoFormGet400ResponseApiDto build() {
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

