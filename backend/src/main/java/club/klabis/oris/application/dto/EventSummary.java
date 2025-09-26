package club.klabis.oris.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EventSummary(
        @JsonProperty("ID")
        int id,
        @JsonProperty("Name")
        String name,
        @JsonProperty("Date")
        LocalDate date,
        @JsonProperty("Place")
        String location,
        @JsonProperty("Org1")
        Organizer organizer1,
        @JsonProperty("Level")
        Level level,
        @JsonProperty("Sport")
        Sport sport,
        @JsonProperty("Discipline")
        Discipline discipline,
        @JsonProperty("EntryDate1")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime entryDate1,
        @JsonProperty("EntryDate2")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime entryDate2,
        @JsonProperty("EntryDate3")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime entryDate3
) {
}
