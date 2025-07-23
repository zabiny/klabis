package club.klabis.adapters.api;

import club.klabis.users.domain.ApplicationUser;
import net.minidev.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping(value = "/api/test", produces = MediaType.APPLICATION_JSON_VALUE)
    //@HasGrant(ApplicationGrant.MEMBERS_REGISTER)
    public JSONObject getResponse(@AuthenticationPrincipal ApplicationUser principal) {
        return new JSONObject(Map.of("message", "Hello %s!".formatted(principal.getUsername())));
    }

}
