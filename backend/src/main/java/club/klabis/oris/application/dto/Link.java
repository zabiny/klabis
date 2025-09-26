package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Link(
        @JsonProperty("ID")
        String id,

        @JsonProperty("Url")
        String url,

        @JsonProperty("SourceType")
        SourceType sourceType,

        @JsonProperty("OtherDescCZ")
        String otherDescCZ,

        @JsonProperty("OtherDescEN")
        String otherDescEN
) {
}
