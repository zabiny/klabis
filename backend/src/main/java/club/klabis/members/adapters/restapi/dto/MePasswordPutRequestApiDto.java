package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * MePasswordPutRequestApiDto
 */

@JsonTypeName("_me_password_put_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MePasswordPutRequestApiDto extends RepresentationModel<MePasswordPutRequestApiDto> {

    private String password;

    public MePasswordPutRequestApiDto password(String password) {
        this.password = password;
        return this;
    }

    /**
     * The new password
     *
     * @return password
     */

    @Schema(name = "password", description = "The new password", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MePasswordPutRequestApiDto mePasswordPutRequest = (MePasswordPutRequestApiDto) o;
        return Objects.equals(this.password, mePasswordPutRequest.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(password);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MePasswordPutRequestApiDto {\n");
        sb.append("    password: ").append(toIndentedString(password)).append("\n");
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

        private MePasswordPutRequestApiDto instance;

        public Builder() {
            this(new MePasswordPutRequestApiDto());
        }

        protected Builder(MePasswordPutRequestApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MePasswordPutRequestApiDto value) {
            this.instance.setPassword(value.password);
            return this;
        }

        public Builder password(String password) {
            this.instance.password(password);
            return this;
        }

        /**
         * returns a built MePasswordPutRequestApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MePasswordPutRequestApiDto build() {
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

