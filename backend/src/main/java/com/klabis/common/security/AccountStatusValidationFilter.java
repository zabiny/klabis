package com.klabis.common.security;

import tools.jackson.databind.ObjectMapper;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter that validates the authenticated user's account status.
 * <p>
 * Ensures that suspended or deactivated users cannot use JWT tokens to access the API.
 * This filter is applied after JWT decoding but before controller execution.
 * <p>
 * When a member is suspended, their User account is suspended. This filter checks
 * the current account status from the database on each request and returns HTTP 403 Forbidden
 * if the user is no longer authenticatable (e.g., account is suspended).
 */
public class AccountStatusValidationFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public AccountStatusValidationFilter(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication instanceof KlabisJwtAuthenticationToken jwtAuth) {
            String username = jwtAuth.getUsername();

            if (userService.findUserByUsername(username).isPresent()) {
                User user = userService.findUserByUsername(username).get();
                if (!user.isAuthenticatable()) {
                    handleDisabledUser(response);
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private void handleDisabledUser(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> problem = new HashMap<>();
        problem.put("type", "https://api.klabis.example.com/errors/forbidden");
        problem.put("title", "Forbidden");
        problem.put("status", 403);
        problem.put("detail", "User account is no longer active");

        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
