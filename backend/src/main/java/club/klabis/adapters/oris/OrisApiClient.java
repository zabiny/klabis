package club.klabis.adapters.oris;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public interface OrisApiClient {

    int CLUB_ID_ZBM = 205;

    String REGION_JIHOMORAVSKA = "JM";
    String REGION_CR = "ČR";

    @Cacheable(cacheNames = "oris:getUser")
    @GetExchange("/API/?format=json&method=getUser")
    OrisResponse<OrisUserInfo> getUserInfo(@RequestParam("rgnum") String registrationNumber);

    @Cacheable(cacheNames = "oris:eventList")
    @GetExchange("/API/?format=json&method=getEventList&myClubId=" + CLUB_ID_ZBM)
    OrisResponse<Map<String, OrisEvent>> getEventList(OrisEventListFilter filter);

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

    record OrisEventListFilter(
            String region,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        public static OrisEventListFilter EMPTY = new OrisEventListFilter(null, null, null);

        public static OrisEventListFilter createDefault() {
            return new OrisEventListFilter(OrisApiClient.REGION_JIHOMORAVSKA, LocalDate.now(), LocalDate.now().plusMonths(3));
        }

        public OrisEventListFilter withRegion(String region) {
            return new OrisEventListFilter(region, dateFrom, dateTo);
        }

        public OrisEventListFilter withDateTo(LocalDate dateTo) {
            return new OrisEventListFilter(region, dateFrom, dateTo);
        }

        public OrisEventListFilter withDateFrom(LocalDate dateFrom) {
            return new OrisEventListFilter(region, dateFrom, dateTo);
        }
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
        public static int ID_OB = 1;
        public static int ID_LOB = 2;
        public static int ID_MTBO = 3;
        public static int ID_TRAIL = 4;
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
