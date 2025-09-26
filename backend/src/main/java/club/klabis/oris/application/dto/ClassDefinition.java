package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ClassDefinition(
        @JsonProperty("ID")
        String id,

        @JsonProperty("AgeFrom")
        String ageFrom,

        @JsonProperty("AgeTo")
        String ageTo,

        @JsonProperty("Gender")
        String gender,

        @JsonProperty("Name")
        String name
) {
}
