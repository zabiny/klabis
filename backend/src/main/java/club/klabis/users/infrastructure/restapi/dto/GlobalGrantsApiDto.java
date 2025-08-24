package club.klabis.users.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Generated;

/**
 * Global grants are assigned to users and are valid globally in the application.  | Grant name | granted permissions | | --- | --- | | `members:register` | can create new members | | `members:edit` | can edit selected attributes for all existing members | | `members:suspendMembership` | can suspend membership for club members | | `members:permissions` | can change global grants for any member |
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public enum GlobalGrantsApiDto {

    REGISTER("members:register"),

    EDIT("members:edit"),

    SUSPENDMEMBERSHIP("members:suspendMembership"),

    PERMISSIONS("members:permissions");

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

