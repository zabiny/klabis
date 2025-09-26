package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SourceType(
        @JsonProperty("ID")
        String id,

        @JsonProperty("NameCZ")
        String nameCZ,

        @JsonProperty("NameEN")
        String nameEN
) {
}
