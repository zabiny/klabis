package com.klabis.common.ui;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
        validatedByContent(registry.addResourceHandler("/sw.js", "/manifest.webmanifest"))
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache().mustRevalidate());

        // SPA shell and silent-renew page must be revalidated on every request.
        // A cached shell can reference hashed assets removed by a deploy,
        // which renders a blank page. PathPattern specificity makes these
        // handlers win over the /** handler below.
        validatedByContent(registry.addResourceHandler("/index.html", "/silent-renew.html"))
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache());

        // Remaining static files (favicon, pwa-*.png, robots.txt, ...).
        // Short cache so a logo swap propagates within the hour without manual purge.
        validatedByContent(registry.addResourceHandler("/**"))
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic());
    }

    // Buildpacks (bootBuildImage) reset every file's mtime to 1980-01-01, so Last-Modified never
    // changes between releases and revalidation answers 304 with stale content. A content hash
    // ETag changes exactly when the file does.
    private static ResourceHandlerRegistration validatedByContent(ResourceHandlerRegistration registration) {
        return registration
                .setUseLastModified(false)
                .setEtagGenerator(FrontendConfiguration::contentHash);
    }

    private static String contentHash(Resource resource) {
        try (InputStream content = resource.getInputStream()) {
            return DigestUtils.md5DigestAsHex(content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

@Controller
class FrontendController {

    @GetMapping("/auth/callback")
    public String authCallback() {
        return "forward:/index.html";
    }

}
