package com.klabis.common.exceptions;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public interface UserFacingError {

    default String errorType() {
        String name = getClass().getSimpleName();
        if (name.endsWith("Exception")) {
            name = name.substring(0, name.length() - "Exception".length());
        }
        return name.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }

    /**
     * Every error type this exception can emit, readable without a thrown instance. Only exceptions whose
     * {@link #errorType()} depends on how they were raised need to override it.
     */
    default Set<String> errorTypes() {
        return Set.of(errorType());
    }

    default Map<String, Object> messageParameters() {
        return Map.of();
    }

    default @Nullable String field() {
        return null;
    }
}
