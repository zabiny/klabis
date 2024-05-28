package club.klabis.adapters.api;

import net.minidev.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping(value = "/api/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONObject getResponse(Authentication principal) {
        return new JSONObject(Map.of("message", "Hello %s!".formatted(principal.getName())));    }

}
