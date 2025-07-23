package club.klabis.oris.adapters.apiclient;


import club.klabis.oris.domain.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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

}
