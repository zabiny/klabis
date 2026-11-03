package com.klabis.common.patch;

import org.openapitools.jackson.nullable.JsonNullable;

import java.util.function.Consumer;

/**
 * Applies PATCH request fields onto a pre-filled baseline.
 *
 * <p>A {@link JsonNullable} request field is in one of three states: undefined (absent from the
 * request body), present null (sent as {@code null}) and present value. Which states change the
 * baseline depends on whether the target field can be cleared:
 * <ul>
 *   <li>{@link #overlay} is for a field that can be cleared. A present field is applied, including
 *       a present null, which clears it. An undefined field keeps the baseline.</li>
 *   <li>{@link #overlayNonNull} is for a field with no cleared state. Only a present non-null value
 *       is applied. A present null and an undefined field both keep the baseline.</li>
 * </ul>
 */
public final class PatchOverlay {

    private PatchOverlay() {
    }

    public static <T> void overlay(JsonNullable<T> field, Consumer<T> apply) {
        field.ifPresent(apply);
    }

    public static <T> void overlayNonNull(JsonNullable<T> field, Consumer<T> apply) {
        if (field.isPresent() && field.get() != null) {
            apply.accept(field.get());
        }
    }
}
