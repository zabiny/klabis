package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.exceptions.CommonErrorTypes;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;

/**
 * Maps a bad {@code {entityType}} path segment of {@link SynchronizationController}. Scoped to that
 * controller via {@code assignableTypes} because {@link MethodArgumentTypeMismatchException} is a generic
 * Spring MVC exception: an unscoped advice would intercept unrelated controllers' binding failures too.
 */
@MvcComponent
@RestControllerAdvice(assignableTypes = SynchronizationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class SyncExceptionHandler {

    /**
     * A {@code {entityType}} segment outside {@code SyncEntityTypeParam}'s declared
     * values fails Spring MVC's own path-variable binding before any handler method
     * runs (design.md D14's "rejects unknown segments... before any handler runs",
     * satisfied literally here — see {@link SyncEntityTypeResolver}'s javadoc for why
     * this differs from D14's routing-level phrasing). The default outcome for a
     * binding failure is 400; this maps it to 404, matching every other 404 case on
     * these endpoints ("nothing here matches what you asked for"). Safe to handle
     * unconditionally — the class-level {@code assignableTypes} restricts this whole
     * advice to {@link SynchronizationController}, whose only {@code @PathVariable}
     * enum is {@code entityType}. A mismatch on a query parameter (filters of the attempts
     * feed) is a plain 400.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ApiResponse(
            responseCode = "404",
            description = "Resource not found - the entityType path segment names no synchronisable entity type",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if (ex.getParameter().getParameterAnnotation(PathVariable.class) == null) {
            return ErrorResponse.builder(ex, HttpStatusCode.valueOf(400), "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue())
                    .title("Bad Request")
                    .type(URI.create(CommonErrorTypes.INVALID_REQUEST))
                    .build();
        }
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(404), "Unknown synchronisable entity type: " + ex.getValue())
                .title("Resource Not Found")
                .type(URI.create(CommonErrorTypes.NOT_FOUND))
                .build();
    }
}
