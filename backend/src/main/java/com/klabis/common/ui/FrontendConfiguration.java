package com.klabis.common.ui;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class FrontendConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Vite emits content-addressed filenames (index-<hash>.js).
        // A new release ships under a new name, so we can cache aggressively.
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

        // Service worker + PWA manifest drive update detection.
        // Must revalidate every request — otherwise the browser keeps an old SW
        // and registration.update() returns 304, blocking the new release from propagating.
        registry.addResourceHandler("/sw.js", "/workbox-*.js", "/registerSW.js", "/manifest.webmanifest")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache().mustRevalidate());

        // Remaining static files (favicon, pwa-*.png, robots.txt, ...).
        // Short cache so a logo swap propagates within the hour without manual purge.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic());
    }
}

@Controller
class FrontendController {

    @GetMapping("/auth/callback")
    public String authCallback() {
        return "forward:/index.html";
    }

}
