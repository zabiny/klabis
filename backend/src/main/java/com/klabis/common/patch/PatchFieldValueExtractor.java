package com.klabis.common.patch;

import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

/**
 * Enables Jakarta Bean Validation cascade (@Valid) into the value held by a PatchField.
 * <p>
 * When a field is annotated with {@code @Valid PatchField<SomeDto>}, the validator will
 * call this extractor to get the inner value and validate it if the PatchField is provided.
 * If the PatchField is not provided or holds null, no cascade validation occurs.
 */
class PatchFieldValueExtractor implements ValueExtractor<PatchField<@ExtractedValue ?>> {

    @Override
    public void extractValues(PatchField<?> originalValue, ValueReceiver receiver) {
        if (originalValue != null && originalValue.isProvided()) {
            receiver.value(null, originalValue.throwIfNotProvided());
        }
    }
}
