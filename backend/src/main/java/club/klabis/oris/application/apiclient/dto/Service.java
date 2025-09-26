package club.klabis.oris.application.apiclient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Service(
        @JsonProperty("ID")
        String id,

        @JsonProperty("NameCZ")
        String nameCZ,

        @JsonProperty("NameEN")
        String nameEN,

        @JsonProperty("LastBookingDateTime")
        String lastBookingDateTime,

        @JsonProperty("UnitPrice")
        String unitPrice,

        @JsonProperty("QtyAvailable")
        String qtyAvailable,

        @JsonProperty("QtyAlreadyOrdered")
        Integer qtyAlreadyOrdered,

        @JsonProperty("QtyRemaining")
        Integer qtyRemaining
) {
}
