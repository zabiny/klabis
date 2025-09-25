package club.klabis.oris.application.apiclient;


import club.klabis.oris.application.apiclient.dto.OrisEvent;
import club.klabis.oris.application.apiclient.dto.OrisEventListFilter;
import club.klabis.oris.application.apiclient.dto.OrisUserInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.time.LocalDateTime;
import java.util.Map;

// TODO: move into adapters
@SecondaryPort
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

}
