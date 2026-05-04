package com.klabis.common.ui;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.List;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class SpaFallbackFilter extends OncePerRequestFilter {

    private static final List<String> EXCLUDED_PREFIXES = List.of(
            "/api/", "/swagger-ui/", "/v3/api-docs", "/docs/",
            "/oauth2/", "/.well-known/", "/actuator/", "/h2-console/",
            "/login", "/logout", "/error"
    );

    private static final List<String> EXCLUDED_EXACT = List.of(
            "/silent-renew.html", "/swagger-ui.html"
    );

    private static final List<String> STATIC_ASSET_EXTENSIONS = List.of(
            ".js", ".css", ".map", ".ico", ".png", ".jpg", ".jpeg",
            ".svg", ".webp", ".woff", ".woff2", ".ttf", ".json",
            ".webmanifest", ".txt", ".html"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"GET".equalsIgnoreCase(request.getMethod()) || !acceptsHtml(request)) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        chain.doFilter(request, wrapper);

        if (wrapper.getStatus() == HttpServletResponse.SC_NOT_FOUND && isSpaCandidate(request)) {
            response.reset();
            request.getRequestDispatcher("/index.html").forward(request, response);
        } else {
            wrapper.copyBodyToResponse();
        }
    }

    private boolean acceptsHtml(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("text/html");
    }

    private boolean isSpaCandidate(HttpServletRequest request) {
        String path = request.getRequestURI();

        if (EXCLUDED_EXACT.contains(path)) {
            return false;
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        for (String ext : STATIC_ASSET_EXTENSIONS) {
            if (path.endsWith(ext)) {
                return false;
            }
        }
        return true;
    }
}
