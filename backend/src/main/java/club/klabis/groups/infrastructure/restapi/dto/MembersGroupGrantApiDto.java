package club.klabis.groups.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Generated;

/**
 * Gets or Sets MembersGroupGrant
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public enum MembersGroupGrantApiDto {

    FINANCE_DISPLAYFINANCE("finance#displayFinance"),

    FINANCE_MANAGEFINANCE("finance#manageFinance"),

    EVENTAPPLICATIONS_SHOWEVENTAPPLICATIONS("eventApplications#showEventApplications"),

    EVENTAPPLICATIONS_MANAGEEVENTAPPLICATIONS("eventApplications#manageEventApplications"),

    MEMBERINFO_DISPLAYMEMBERINFO("memberInfo#displayMemberInfo"),

    MEMBERINFO_MANAGEMEMBERINFO("memberInfo#manageMemberInfo"),

    GROUPS_INVITEINTOGROUP("groups#inviteIntoGroup"),

    NEWS_PUBLISHNEWSFORGROUP("news#publishNewsForGroup");

    private String value;

    MembersGroupGrantApiDto(String value) {
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
    public static MembersGroupGrantApiDto fromValue(String value) {
        for (MembersGroupGrantApiDto b : MembersGroupGrantApiDto.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}

