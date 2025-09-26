package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Organizer(
        @JsonProperty("ID")
        int id,
        @JsonProperty("Abbr")
        String abbreviation,
        @JsonProperty("Name")
        String name
) {
}
