package com.dpolach.api.orisclient;

import com.dpolach.api.orisclient.dto.EventDetails;
import com.dpolach.api.orisclient.dto.EventEntry;
import com.dpolach.api.orisclient.dto.EventSummary;
import com.dpolach.api.orisclient.dto.OrisUserInfo;
import com.dpolach.api.orisclient.dto.lov.ClubContactTypeListEntry;
import com.dpolach.api.orisclient.dto.lov.DisciplineListEntry;
import com.dpolach.api.orisclient.dto.lov.LevelListEntry;
import com.dpolach.api.orisclient.dto.lov.RegionListEntry;
import com.dpolach.api.orisclient.dto.lov.SourceTypeListEntry;
import com.dpolach.api.orisclient.dto.lov.SportListEntry;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public interface OrisApiClient {

    @Cacheable(cacheNames = "oris:getUser")
    @GetExchange("/API/?format=json&method=getUser")
    OrisResponse<OrisUserInfo> getUserInfo(@RequestParam("rgnum") String registrationNumber);

    @Cacheable(cacheNames = "oris:eventList")
    @GetExchange("/API/?format=json&method=getEventList")
    OrisResponse<Map<String, EventSummary>> getEventList(OrisEventListFilter filter);

    @Cacheable(cacheNames = "oris:event")
    @GetExchange("/API/?format=json&method=getEvent")
    OrisResponse<EventDetails> getEventDetails(@RequestParam("id") int eventId);

    @GetExchange("/API/?format=json&method=getEventEntries")
    OrisResponse<Map<String, EventEntry>> getEventEntries(@RequestParam("eventid") int eventId, @RequestParam("clubid") Integer clubId);

    @Cacheable(cacheNames = "oris:lov:sport")
    @GetExchange("/API/?format=json&method=getList&list=sport")
    OrisResponse<Map<String, SportListEntry>> listSports();

    @Cacheable(cacheNames = "oris:lov:region")
    @GetExchange("/API/?format=json&method=getList&list=region")
    OrisResponse<Map<String, RegionListEntry>> listRegions();

    @Cacheable(cacheNames = "oris:lov:discipline")
    @GetExchange("/API/?format=json&method=getList&list=discipline")
    OrisResponse<Map<String, DisciplineListEntry>> listDisciplines();

    @Cacheable(cacheNames = "oris:lov:level")
    @GetExchange("/API/?format=json&method=getList&list=level")
    OrisResponse<Map<String, LevelListEntry>> listLevels();

    @Cacheable(cacheNames = "oris:lov:sourcetype")
    @GetExchange("/API/?format=json&method=getList&list=sourcetype")
    OrisResponse<Map<String, SourceTypeListEntry>> listSourceTypes();

    @Cacheable(cacheNames = "oris:lov:clubcontacttype")
    @GetExchange("/API/?format=json&method=getList&list=clubcontacttype")
    OrisResponse<Map<String, ClubContactTypeListEntry>> listClubContactTypes();

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
