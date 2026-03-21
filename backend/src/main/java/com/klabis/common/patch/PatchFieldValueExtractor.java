package com.klabis.common.patch;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.UnwrapByDefault;
import jakarta.validation.valueextraction.ValueExtractor;

/**
 * Enables Jakarta Bean Validation to validate the value held by a PatchField.
 * <p>
 * {@code @UnwrapByDefault} causes standard constraint annotations ({@code @NotBlank},
 * {@code @Size}, {@code @Pattern}) placed directly on a {@code PatchField<T>} field to
 * apply to the unwrapped value rather than the wrapper itself. Null values are also
 * extracted when a PatchField is provided, so {@code @NotBlank}/{@code @NotNull} can
 * produce violations for explicitly-provided nulls.
 * <p>
 * Validation is skipped entirely when the PatchField is not provided — the extractor
 * simply does not call {@code receiver.value()}, so no constraints are evaluated.
 * <p>
 * {@code @Valid} cascade into nested DTOs continues to work as before.
 */
@UnwrapByDefault
public class PatchFieldValueExtractor implements ValueExtractor<PatchField<@ExtractedValue ?>> {

    @Override
    public void extractValues(PatchField<?> originalValue, ValueReceiver receiver) {
        if (originalValue != null && originalValue.isProvided()) {
            receiver.value(null, originalValue.patchValue(null));
        }
    }
}
