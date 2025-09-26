package club.klabis.oris.application.apiclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Document(
        @JsonProperty("ID")
        String id,

        @JsonProperty("Name")
        String name,

        @JsonProperty("Url")
        String url
) {
}
