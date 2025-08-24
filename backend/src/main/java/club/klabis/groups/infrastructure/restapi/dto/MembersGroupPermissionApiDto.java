package club.klabis.groups.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * Single group permission
 */

@Schema(name = "MembersGroupPermission", description = "Single group permission")
@JsonTypeName("MembersGroupPermission")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MembersGroupPermissionApiDto extends RepresentationModel<MembersGroupPermissionApiDto> {

    private MembersGroupGrantApiDto permission;

    private Boolean grantedToOwner;

    private Boolean grantedToMember;

    public MembersGroupPermissionApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public MembersGroupPermissionApiDto(MembersGroupGrantApiDto permission) {
        this.permission = permission;
    }

    public MembersGroupPermissionApiDto permission(MembersGroupGrantApiDto permission) {
        this.permission = permission;
        return this;
    }

    /**
     * Get permission
     *
     * @return permission
     */
    @NotNull
    @Valid
    @Schema(name = "permission", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("permission")
    public MembersGroupGrantApiDto getPermission() {
        return permission;
    }

    public void setPermission(MembersGroupGrantApiDto permission) {
        this.permission = permission;
    }

    public MembersGroupPermissionApiDto grantedToOwner(Boolean grantedToOwner) {
        this.grantedToOwner = grantedToOwner;
        return this;
    }

    /**
     * tells if permission is granted to group owner
     *
     * @return grantedToOwner
     */

    @Schema(name = "grantedToOwner", description = "tells if permission is granted to group owner", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("grantedToOwner")
    public Boolean isGrantedToOwner() {
        return grantedToOwner;
    }

    public void setGrantedToOwner(Boolean grantedToOwner) {
        this.grantedToOwner = grantedToOwner;
    }

    public MembersGroupPermissionApiDto grantedToMember(Boolean grantedToMember) {
        this.grantedToMember = grantedToMember;
        return this;
    }

    /**
     * tells if permission is granted to group members
     *
     * @return grantedToMember
     */

    @Schema(name = "grantedToMember", description = "tells if permission is granted to group members", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("grantedToMember")
    public Boolean isGrantedToMember() {
        return grantedToMember;
    }

    public void setGrantedToMember(Boolean grantedToMember) {
        this.grantedToMember = grantedToMember;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MembersGroupPermissionApiDto membersGroupPermission = (MembersGroupPermissionApiDto) o;
        return Objects.equals(this.permission, membersGroupPermission.permission) &&
               Objects.equals(this.grantedToOwner, membersGroupPermission.grantedToOwner) &&
               Objects.equals(this.grantedToMember, membersGroupPermission.grantedToMember);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permission, grantedToOwner, grantedToMember);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MembersGroupPermissionApiDto {\n");
        sb.append("    permission: ").append(toIndentedString(permission)).append("\n");
        sb.append("    grantedToOwner: ").append(toIndentedString(grantedToOwner)).append("\n");
        sb.append("    grantedToMember: ").append(toIndentedString(grantedToMember)).append("\n");
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

        private MembersGroupPermissionApiDto instance;

        public Builder() {
            this(new MembersGroupPermissionApiDto());
        }

        protected Builder(MembersGroupPermissionApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MembersGroupPermissionApiDto value) {
            this.instance.setPermission(value.permission);
            this.instance.setGrantedToOwner(value.grantedToOwner);
            this.instance.setGrantedToMember(value.grantedToMember);
            return this;
        }

        public Builder permission(MembersGroupGrantApiDto permission) {
            this.instance.permission(permission);
            return this;
        }

        public Builder grantedToOwner(Boolean grantedToOwner) {
            this.instance.grantedToOwner(grantedToOwner);
            return this;
        }

        public Builder grantedToMember(Boolean grantedToMember) {
            this.instance.grantedToMember(grantedToMember);
            return this;
        }

        /**
         * returns a built MembersGroupPermissionApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MembersGroupPermissionApiDto build() {
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

