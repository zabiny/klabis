package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Generated;

/**
 * Member specific grants are defined between 2 users (user is allowed to perform specific action on behalf of another user). These define fine-grained permissions and can be granted explicitely to selected users or through permissions granted from membership between members of user groups.  | Grant name | granted permissions | | --- | --- | | `members#canDisplayMemberPersonalContact` | can display personal contact information of member | | `members#canDisplayMemberLegalGuardianContact` | can display contact information of legal guardian of member | | `members#canDisplayMemberAddress` | can display contact information of legal guardian of member |
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public enum MemberSpecificGrantsApiDto {

    CANDISPLAYMEMBERPERSONALCONTACT("members#canDisplayMemberPersonalContact"),

    CANDISPLAYMEMBERLEGALGUARDIANCONTACT("members#canDisplayMemberLegalGuardianContact"),

    CANDISPLAYMEMBERADDRESS("members#canDisplayMemberAddress");

    private String value;

    MemberSpecificGrantsApiDto(String value) {
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
    public static MemberSpecificGrantsApiDto fromValue(String value) {
        for (MemberSpecificGrantsApiDto b : MemberSpecificGrantsApiDto.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}

