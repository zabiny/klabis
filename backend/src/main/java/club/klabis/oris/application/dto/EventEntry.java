package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.soabase.recordbuilder.core.RecordBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.exc.InvalidFormatException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RecordBuilder
public record EventEntry(
        @JsonProperty("ID") Integer id,
        @JsonProperty("ClassID") Integer classId,
        @JsonProperty("ClassDesc") String classDesc,
        @JsonProperty("RegNo") String regNo,
        @JsonProperty("Name") String name,
        @JsonProperty("FirstName") String firstName,
        @JsonProperty("LastName") String lastName,
        @JsonDeserialize(using = BooleanFromIntDeserializer.class)
        @JsonProperty("RentSI") Boolean rentSi,
        @JsonProperty("SI") Integer si,
        @JsonProperty("Licence") String licence,
        @JsonProperty("RequestedStart") String requestedStart,
        @JsonProperty("UserID") Integer userId,
        @JsonProperty("ClubUserID") Integer clubUserId,
        @JsonProperty("ClubID") Integer clubId,
        @JsonProperty("Note") String note,
        @JsonProperty("Nationality") String nationality,
        @JsonProperty("Fee") BigDecimal fee,
        @JsonProperty("EntryStop") int entryStop,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonProperty("CreatedDateTime") LocalDateTime createdDateTime,
        @JsonProperty("CreatedByUserID") int createdByUserId,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonProperty("UpdatedDateTime") LocalDateTime updatedDateTime,
        @JsonProperty("UpdatedByUserID") Integer updatedByUserId
) {

    static class BooleanFromIntDeserializer extends ValueDeserializer<Boolean> {
        @Override
        public Boolean deserialize(tools.jackson.core.JsonParser p, DeserializationContext ctxt) throws JacksonException {
            int value = Integer.parseInt(p.getString());
            if (value == 1) return true;
            if (value == 0) return false;
            throw new InvalidFormatException(p,
                    "Unexpected value for 'boolean-int' conversion, expecting '0' or '1'",
                    value,
                    Boolean.class);
        }
    }
}

