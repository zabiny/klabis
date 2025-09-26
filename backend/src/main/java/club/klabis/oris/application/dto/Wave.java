package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Wave(
        @JsonProperty("ID")
        String id,

        @JsonProperty("Name")
        String name,

        @JsonProperty("StartTime")
        String startTime
) {
}
