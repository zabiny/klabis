package club.klabis.oris.application.apiclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrisEventSport(
        @JsonProperty("ID")
        int id,
        @JsonProperty("NameCZ")
        String nameCZ,
        @JsonProperty("NameEN")
        String nameEN
) {
    public static int ID_OB = 1;
    public static int ID_LOB = 2;
    public static int ID_MTBO = 3;
    public static int ID_TRAIL = 4;
}
