package com.klabis.oris.apiclient;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.klabis.oris.apiclient.dto.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface OrisApiClient {

    int CLUB_ID_ZBM = 205;
    String REGION_JIHOMORAVSKA = "JM";
    String REGION_CR = "ČR";

    int DISCIPLINE_ID_LONG_DISTANCE = 1;
    int DISCIPLINE_ID_SHORT_DISTANCE = 2;
    int DISCIPLINE_ID_SPRINT = 3;
    int DISCIPLINE_ID_ULTRALONG_DISTANCE = 4;
    int DISCIPLINE_ID_RELAYS = 5;

    @Cacheable(cacheNames = "oris:getUser")
    @GetExchange("/API/?format=json&method=getUser")
    OrisResponse<OrisUserInfo> getUserInfo(@RequestParam("rgnum") String registrationNumber);

    @Cacheable(cacheNames = "oris:eventList")
    @GetExchange("/API/?format=json&method=getEventList&myClubId=" + CLUB_ID_ZBM)
    OrisResponse<Map<String, EventSummary>> getEventList(OrisEventListFilter filter);

    @Cacheable(cacheNames = "oris:event")
    @GetExchange("/API/?format=json&method=getEvent")
    OrisResponse<EventDetails> getEventDetails(@RequestParam("id") int eventId);

    @GetExchange("/API/?format=json&method=getEventEntries")
    OrisResponse<Map<String, EventEntry>> getEventEntries(@RequestParam("eventid") int eventId, @RequestParam("clubid") Integer clubId);

    record OrisResponse<T>(
            @JsonProperty("Data") @Nullable T data,
            @JsonProperty("Format") String format,
            @JsonProperty("Status") String status,
            @JsonProperty("ExportCreated") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime exportCreated,
            @JsonProperty("Method") String method
    ) {
        public Optional<T> payload() {
            if ("OK".equals(status)) {
                return Optional.ofNullable(data);
            } else {
                throw new IllegalStateException("ORIS API returned error response with status %s".formatted(status));
            }
        }
    }
}
