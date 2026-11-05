package com.klabis.common.mvc;

import com.klabis.common.exceptions.CommonErrorTypes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

/**
 * Registered after all other resolvers instead of a catch-all {@code @ExceptionHandler}, so it cannot shadow
 * the exception handlers of other modules' advices.
 */
class UnexpectedErrorResolver implements HandlerExceptionResolver {

    private static final Logger LOG = LoggerFactory.getLogger(UnexpectedErrorResolver.class);

    private final ObjectMapper objectMapper;

    UnexpectedErrorResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public @Nullable ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                                   @Nullable Object handler, Exception ex) {
        if (response.isCommitted() || ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            return null;
        }

        LOG.error("Unhandled exception: {}", ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
        problem.setType(URI.create(CommonErrorTypes.UNEXPECTED));
        try {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(problem));
        } catch (IOException ioe) {
            LOG.warn("Cannot write error response", ioe);
            return null;
        }
        return new ModelAndView();
    }
}
