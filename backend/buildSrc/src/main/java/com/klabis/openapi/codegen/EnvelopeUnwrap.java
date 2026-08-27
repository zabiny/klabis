package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.media.Schema;

/**
 * Result of {@link HalEnvelopeDetector#detect}: the schema a HAL envelope should be unwrapped to,
 * plus enough shape information for {@code handleMethodResponse()} to pick the right Java return
 * type ({@code List<T>} or the bare payload {@code T} — whether the container is instead
 * {@code Page<T>} is NOT decided here, see design.md Decision 2: pagination is a property of the
 * operation, via {@code x-spring-paginated}, not of one response representation's shape).
 *
 * @param targetSchema an unresolved {@code $ref} schema pointing at the payload the envelope
 *                      should unwrap to — kept as a {@code $ref} (not resolved) so the stock
 *                      generator's own type resolution, which reads the schema name off the
 *                      {@code $ref}, can map it onto the right Java class
 * @param isCollection {@code true} for Shape 2 ({@code PagedModel}/{@code CollectionModel}) — the
 *                      unwrapped type is a container of {@code targetSchema}, not the schema itself
 */
public record EnvelopeUnwrap(Schema<?> targetSchema, boolean isCollection) {
}
