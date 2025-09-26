package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Region(
        @JsonProperty("ID")
        String id,

        @JsonProperty("Name")
        String name
) {
}
