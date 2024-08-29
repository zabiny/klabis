package club.klabis.adapters.oris;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.time.LocalDateTime;

public interface OrisApiClient {

    @GetExchange("/API/?format=json&method=getUser")
    OrisResponse<OrisUserInfo> getUserInfo(@RequestParam("rgnum") String registrationNumber);

    record OrisResponse<T> (
            @JsonProperty("Data") T data,
            @JsonProperty("Format") String format,
            @JsonProperty("Status") String status,
            @JsonProperty("ExportCreated") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime exportCreated
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
}
