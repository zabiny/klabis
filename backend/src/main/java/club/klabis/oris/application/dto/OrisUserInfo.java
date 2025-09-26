package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrisUserInfo(
        @JsonProperty("ID")
        int orisId,
        @JsonProperty("FirstName")
        String firstName,
        @JsonProperty("LastName")
        String lastName) {
}
