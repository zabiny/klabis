package club.klabis.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * RFC7807ErrorResponseApiDto
 */

@JsonTypeName("RFC7807ErrorResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class RFC7807ErrorResponseApiDto extends RepresentationModel<RFC7807ErrorResponseApiDto> {

    private String title;

    private Integer status;

    private String detail;

    private String instance;

    private String type;

    public RFC7807ErrorResponseApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public RFC7807ErrorResponseApiDto(String title, Integer status, String detail, String instance) {
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
    }

    public RFC7807ErrorResponseApiDto title(String title) {
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

    public RFC7807ErrorResponseApiDto status(Integer status) {
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

    public RFC7807ErrorResponseApiDto detail(String detail) {
        this.detail = detail;
        return this;
    }

    /**
     * User friendly description of the error
     *
     * @return detail
     */
    @NotNull
    @Schema(name = "detail", description = "User friendly description of the error", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("detail")
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public RFC7807ErrorResponseApiDto instance(String instance) {
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

    public RFC7807ErrorResponseApiDto type(String type) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RFC7807ErrorResponseApiDto rfC7807ErrorResponse = (RFC7807ErrorResponseApiDto) o;
        return Objects.equals(this.title, rfC7807ErrorResponse.title) &&
               Objects.equals(this.status, rfC7807ErrorResponse.status) &&
               Objects.equals(this.detail, rfC7807ErrorResponse.detail) &&
               Objects.equals(this.instance, rfC7807ErrorResponse.instance) &&
               Objects.equals(this.type, rfC7807ErrorResponse.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, status, detail, instance, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class RFC7807ErrorResponseApiDto {\n");
        sb.append("    title: ").append(toIndentedString(title)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
        sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
        sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
        sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

        private RFC7807ErrorResponseApiDto instance;

        public Builder() {
            this(new RFC7807ErrorResponseApiDto());
        }

        protected Builder(RFC7807ErrorResponseApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(RFC7807ErrorResponseApiDto value) {
            this.instance.setTitle(value.title);
            this.instance.setStatus(value.status);
            this.instance.setDetail(value.detail);
            this.instance.setInstance(value.instance);
            this.instance.setType(value.type);
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

        /**
         * returns a built RFC7807ErrorResponseApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public RFC7807ErrorResponseApiDto build() {
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

