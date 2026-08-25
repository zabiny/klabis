package com.klabis.openapi.codegen;

import io.swagger.v3.oas.models.media.Schema;

/**
 * Result of {@link HalEnvelopeDetector#detect}: the schema a HAL envelope should be unwrapped to,
 * plus enough shape information for {@code handleMethodResponse()} to pick the right Java return
 * type ({@code Page<T>}, {@code List<T>}, or the bare payload {@code T}).
 *
 * @param targetSchema an unresolved {@code $ref} schema pointing at the payload the envelope
 *                      should unwrap to — kept as a {@code $ref} (not resolved) so the stock
 *                      generator's own type resolution, which reads the schema name off the
 *                      {@code $ref}, can map it onto the right Java class
 * @param isCollection {@code true} for Shape 2 ({@code PagedModel}/{@code CollectionModel}) — the
 *                      unwrapped type is a container of {@code targetSchema}, not the schema itself
 * @param isPaged      {@code true} when a Shape 2 envelope also carries pagination metadata — the
 *                      container is {@code Page<T>} rather than {@code List<T>}
 */
public record EnvelopeUnwrap(Schema<?> targetSchema, boolean isCollection, boolean isPaged) {
}
