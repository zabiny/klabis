package club.klabis.oris.application.apiclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SIType(
        @JsonProperty("ID")
        String id,

        @JsonProperty("Name")
        String name
) {
}
