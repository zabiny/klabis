package com.klabis.common.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.util.Assert;
import org.springframework.web.ErrorResponse;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Writes a {@link UserFacingError} as a problem detail: the type, the exception's own parameters and field,
 * plus parameters only the handler can supply (typically names resolved from identifiers).
 */
public final class UserFacingErrorResponses {

    private static final Set<String> RESERVED_PROPERTIES =
            Set.of("type", "title", "status", "detail", "instance", "field");

    private UserFacingErrorResponses() {
    }

    public static <E extends RuntimeException & UserFacingError> ErrorResponse of(
            E ex, HttpStatusCode status, String title) {
        return of(ex, status, title, Map.of());
    }

    public static <E extends RuntimeException & UserFacingError> ErrorResponse of(
            E ex, HttpStatusCode status, String title, Map<String, ?> handlerParameters) {
        ErrorResponse.Builder builder = ErrorResponse.builder(ex, status, ex.getMessage())
                .title(title)
                .type(URI.create(CommonErrorTypes.uri(ex.errorType())));
        ex.messageParameters().forEach((name, value) -> addParameter(builder, name, value));
        handlerParameters.forEach((name, value) -> addParameter(builder, name, value));
        if (ex.field() != null) {
            builder.property("field", ex.field());
        }
        return builder.build();
    }

    private static void addParameter(ErrorResponse.Builder builder, String name, Object value) {
        Assert.isTrue(!RESERVED_PROPERTIES.contains(name),
                () -> "Message parameter name collides with a ProblemDetail property: " + name);
        builder.property(name, value);
    }
}
