package club.klabis.shared.config.frontend;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping("/auth/callback")
    public String authCallback() {
        return "forward:/index.html";
    }

}
