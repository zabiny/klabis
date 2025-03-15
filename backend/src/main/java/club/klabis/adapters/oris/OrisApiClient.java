package club.klabis.adapters.oris;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface OrisApiClient {

    @Cacheable(cacheNames = "oris:getUser")
    @GetExchange("/API/?format=json&method=getUser")
    OrisResponse<OrisUserInfo> getUserInfo(@RequestParam("rgnum") String registrationNumber);

    @Cacheable(cacheNames = "oris:eventList")
    @GetExchange("/API/?format=json&method=getEventList")
    OrisResponse<Map<String, OrisEvent>> getEventList();

    record OrisResponse<T>(
            @JsonProperty("Data") T data,
            @JsonProperty("Format") String format,
            @JsonProperty("Status") String status,
            @JsonProperty("ExportCreated") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime exportCreated,
            @JsonProperty("Method") String method
    ) {

    }

    record OrisUserInfo(
            @JsonProperty("ID")
            int orisId,
            @JsonProperty("FirstName")
            String firstName,
            @JsonProperty("LastName")
            String lastName) {
    }

    record OrisEvent(
            @JsonProperty("ID")
            int id,
            @JsonProperty("Name")
            String name,
            @JsonProperty("Date")
            LocalDate date,
            @JsonProperty("Place")
            String location,
            @JsonProperty("Org1")
            OrisEventOrg organizer1,
            @JsonProperty("Level")
            OrisEventLevel level,
            @JsonProperty("Sport")
            OrisEventSport sport,
            @JsonProperty("Discipline")
            OrisEventDiscipline discipline,
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

    record OrisEventOrg(
            @JsonProperty("ID")
            int id,
            @JsonProperty("Abbr")
            String abbreviation,
            @JsonProperty("Name")
            String name
    ) {
    }

    record OrisEventLevel(
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

    record OrisEventSport(
            @JsonProperty("ID")
            int id,
            @JsonProperty("NameCZ")
            String nameCZ,
            @JsonProperty("NameEN")
            String nameEN
    ) {

    }

    record OrisEventDiscipline(
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
}
