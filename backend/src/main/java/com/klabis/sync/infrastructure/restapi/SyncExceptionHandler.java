package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.sync.application.ConflictNotAcknowledgedException;
import com.klabis.sync.application.SyncRecordNeedsResolutionException;
import com.klabis.sync.application.SyncRecordNotFailedException;
import com.klabis.sync.application.SyncRecordNotInConflictException;
import com.klabis.sync.application.UnsupportedResolutionException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Maps the sync module's "record needs a decision" exceptions to {@code 409 Conflict}
 * (design.md REST API section) — the shared {@code MvcExceptionHandler} maps
 * {@code BusinessRuleViolationException} (their common base) to {@code 400}, which is
 * right for every other module's business-rule violations but wrong here: all five of
 * these mean "resolve/reset it first", the textbook shape of a 409. Kept local to the
 * sync module rather than changing the shared handler's mapping for everyone, mirroring
 * {@code groups.common.infrastructure.restapi.GroupsExceptionHandler}.
 * <p>
 * Scoped to {@link SynchronizationController} via {@code assignableTypes} — unlike
 * {@code GroupsExceptionHandler}, this advice also handles
 * {@link MethodArgumentTypeMismatchException}, a generic Spring MVC exception any
 * controller's path/query binding can throw. An unscoped {@code @RestControllerAdvice}
 * is global, so without this restriction the mismatch handler below would intercept
 * unrelated controllers' ordinary binding failures too, there is no supported way to
 * "hand a caught exception back" to the next advice in the chain, and 500 (rather than
 * the original 400) is what a bare {@code throw} risks landing on instead. Scoping the
 * advice removes the need for that workaround entirely: a foreign controller's mismatch
 * never reaches this class.
 */
@MvcComponent
@RestControllerAdvice(assignableTypes = SynchronizationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class SyncExceptionHandler {

    @ExceptionHandler(SyncRecordNeedsResolutionException.class)
    @ApiResponse(
            responseCode = "409",
            description = "Conflict - the record needs a decision (resolve the conflict or reset) before it can be synchronised again",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    ErrorResponse handleNeedsResolution(SyncRecordNeedsResolutionException ex) {
        return conflict(ex);
    }

    @ExceptionHandler(SyncRecordNotInConflictException.class)
    @ApiResponse(
            responseCode = "409",
            description = "Conflict - the record is not currently in conflict",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    ErrorResponse handleNotInConflict(SyncRecordNotInConflictException ex) {
        return conflict(ex);
    }

    @ExceptionHandler(ConflictNotAcknowledgedException.class)
    @ApiResponse(
            responseCode = "409",
            description = "Conflict - no current acknowledgement for the record's present conflict",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    ErrorResponse handleNotAcknowledged(ConflictNotAcknowledgedException ex) {
        return conflict(ex);
    }

    @ExceptionHandler(SyncRecordNotFailedException.class)
    @ApiResponse(
            responseCode = "409",
            description = "Conflict - the record is not currently terminally failed",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    ErrorResponse handleNotFailed(SyncRecordNotFailedException ex) {
        return conflict(ex);
    }

    @ExceptionHandler(UnsupportedResolutionException.class)
    @ApiResponse(
            responseCode = "409",
            description = "Conflict - the requested resolution direction is not supported by the integration",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    ErrorResponse handleUnsupportedResolution(UnsupportedResolutionException ex) {
        return conflict(ex);
    }

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
     * enum is {@code entityType}, so no other parameter's mismatch can reach here.
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
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(404), "Unknown synchronisable entity type: " + ex.getValue())
                .title("Resource Not Found")
                .build();
    }

    private static ErrorResponse conflict(RuntimeException ex) {
        return ErrorResponse.builder(ex, HttpStatusCode.valueOf(409), ex.getMessage()).build();
    }
}
