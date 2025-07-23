package club.klabis.oris.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrisEventLevel(
        @JsonProperty("ID")
        int id,
        @JsonProperty("ShortName")
        String shortName,
        @JsonProperty("NameCZ")
        String nameCZ,
        @JsonProperty("NameEN")
        String nameEN
) {

}
