package com.klabis.common.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA fallback controller for React Router.
 * <p>
 * Forwards all non-API HTML requests to the React app's index.html,
 * enabling client-side routing without 404 errors on page refresh.
 * <p>
 * This controller handles routes like:
 * - / (root)
 * - /members
 * - /members/123
 * - /events/456/details
 * <p>
 * Static resources (*.js, *.css, *.png, etc.) are served directly by Spring Boot
 * and are not affected by this controller due to the path pattern exclusion.
 */
@Controller
class SpaFallbackController {

    /**
     * Forward all non-API HTML requests to React app's index.html.
     * <p>
     * Path patterns:
     * - {path:[^\\.]*} - Matches paths without dots (excludes static files like .js, .css)
     * - Supports up to 3-level deep paths (e.g., /events/123/participants)
     * <p>
     * Excluded patterns (handled by Spring Boot static resource handler):
     * - /api/** (REST API endpoints)
     * - /oauth2/** (OAuth2 authorization endpoints)
     * - /actuator/** (Spring Boot Actuator endpoints)
     * - /h2-console/** (H2 database console, dev only)
     * - Static files with extensions (*.js, *.css, *.png, etc.)
     *
     * @return forward directive to index.html
     */
    @GetMapping(value = {
            "/",
            "/{path:(?!h2-console)[^\\.]*}",
            "/{path1:(?!h2-console)[^\\.]*}/{path2:[^\\.]*}",
            "/{path1:(?!h2-console)[^\\.]*}/{path2}/{path3:[^\\.]*}"
    }, produces = "text/html")
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
