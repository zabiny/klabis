package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GetEventDetailsResponse(
        @JsonProperty("Method")
        String method,

        @JsonProperty("Format")
        String format,

        @JsonProperty("Status")
        String status,

        @JsonProperty("ExportCreated")
        String exportCreated,

        @JsonProperty("Data")
        EventDetails data
) {
}
