package com.klabis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Configuration
public class FrontendConfiguration {

}

@Controller
class FrontendController {

    @GetMapping("/auth/callback")
    public String authCallback() {
        return "forward:/index.html";
    }

}