package com.klabis.common.patch;

import org.jspecify.annotations.NonNull;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class PatchField<T> {
    private static final PatchField<?> NOT_PROVIDED = new PatchField<>(null, false);

    private final T value;
    private final boolean provided;

    private PatchField(T value, boolean provided) {
        this.value = value;
        this.provided = provided;
    }

    public static <T> PatchField<T> of(T value) {
        return new PatchField<>(value, true);
    }

    @SuppressWarnings("unchecked")
    public static <T> PatchField<T> notProvided() {
        return (PatchField<T>) NOT_PROVIDED;
    }

    public boolean isProvided() {
        return provided;
    }

    public <O> PatchField<O> map(@NonNull Function<T, O> mapper) {
        if (isProvided()) {
            return PatchField.of(mapper.apply(value));
        } else {
            return PatchField.notProvided();
        }
    }

    public T get() {
        if (!provided) {
            throw new NoSuchElementException("No value provided");
        }
        return value;
    }

    public void ifProvided(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        if (provided) {
            action.accept(value);
        }
    }
}
