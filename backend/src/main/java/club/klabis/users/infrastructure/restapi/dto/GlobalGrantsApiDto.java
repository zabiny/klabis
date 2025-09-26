package club.klabis.users.infrastructure.restapi.dto;

import club.klabis.shared.config.security.ApplicationGrant;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Generated;

/**
 * Global grants are assigned to users and are valid globally in the application.  | Grant name | granted permissions | | --- | --- | | `members:register` | can create new members | | `members:edit` | can edit selected attributes for all existing members | | `members:suspendMembership` | can suspend membership for club members | | `members:permissions` | can change global grants for any member |
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public enum GlobalGrantsApiDto {

    REGISTER(ApplicationGrant.MEMBERS_REGISTER.getGrantName()),

    EDIT(ApplicationGrant.MEMBERS_EDIT.getGrantName()),

    SUSPENDMEMBERSHIP(ApplicationGrant.MEMBERS_SUSPENDMEMBERSHIP.getGrantName()),

    RESUMEMEMBERSHIP(ApplicationGrant.MEMBERS_RESUMEMEMBERSHIP.getGrantName()),

    SYSTEM_ADMIN(ApplicationGrant.SYSTEM_ADMIN.getGrantName()),

    PERMISSIONS(ApplicationGrant.APPUSERS_PERMISSIONS.getGrantName());

    private String value;

    GlobalGrantsApiDto(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static GlobalGrantsApiDto fromValue(String value) {
        for (GlobalGrantsApiDto b : GlobalGrantsApiDto.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}

