package com.klabis.common.ui;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Forwards browser navigation requests to {@code /index.html} when no static resource
 * matches the request path and the path is not reserved for another handler.
 * <p>
 * Decides up front by checking the static resource location: if a file exists, the
 * filter steps aside and the regular {@code ResourceHttpRequestHandler} serves it;
 * otherwise (and only for HTML-accepting GETs to non-reserved paths) the request is
 * forwarded to the SPA shell so client-side routing can render the page.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class SpaFallbackFilter extends OncePerRequestFilter {

    private static final String INDEX_HTML = "/index.html";
    private static final String STATIC_LOCATION = "classpath:/static";

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/api", "/swagger-ui", "/v3/api-docs", "/docs",
            "/oauth2", "/.well-known", "/actuator", "/h2-console",
            "/error"
    );

    private static final List<String> EXCLUDED_EXACT = List.of(
            "/silent-renew.html", "/swagger-ui.html"
    );

    private final ResourceLoader resourceLoader;

    SpaFallbackFilter(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!"GET".equalsIgnoreCase(request.getMethod())
                || isExcluded(path)
                || resourceExists(path)
                || !acceptsHtml(request.getHeader("Accept"))) {
            chain.doFilter(request, response);
            return;
        }

        request.getRequestDispatcher(INDEX_HTML).forward(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getDispatcherType() != DispatcherType.REQUEST;
    }

    private boolean resourceExists(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return resourceLoader.getResource(STATIC_LOCATION + INDEX_HTML).exists();
        }
        return resourceLoader.getResource(STATIC_LOCATION + path).exists();
    }

    private static boolean isExcluded(String path) {
        if (EXCLUDED_EXACT.contains(path)) {
            return true;
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private static boolean acceptsHtml(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank()) {
            return false;
        }
        try {
            for (MediaType mediaType : MediaType.parseMediaTypes(acceptHeader)) {
                if (MediaType.TEXT_HTML.equalsTypeAndSubtype(mediaType)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        return false;
    }
}
