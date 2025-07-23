package club.klabis.oris.application.apiclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrisEventOrg(
        @JsonProperty("ID")
        int id,
        @JsonProperty("Abbr")
        String abbreviation,
        @JsonProperty("Name")
        String name
) {
}
