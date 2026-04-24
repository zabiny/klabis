package com.klabis.common.mvc;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Hides PWA artifacts (manifest, service worker) when the {@code pwa} profile is not active.
 * Returning 404 on the manifest prevents Chrome from offering the install affordance,
 * and 404 on the service worker stops {@code useRegisterSW} from caching the SPA shell.
 */
@Controller
@Profile("!pwa")
class PwaDisabledController {

    @GetMapping({"/manifest.webmanifest", "/sw.js", "/workbox-{hash}.js"})
    public ResponseEntity<Void> notFound() {
        return ResponseEntity.notFound().build();
    }
}
