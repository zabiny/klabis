package com.klabis.common.ui;

import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared reflection helpers for auto-detecting HAL-FORMS enum options: unwrapping a record
 * component's declared type down to the underlying enum (through {@link JsonNullable},
 * {@link Optional} and any {@link Collection}, in any nesting), and reading the enum constants'
 * serialized JSON values in declaration order.
 * <p>
 * Used by {@link EnumOptionsAutoConfiguration} to register options directly with Spring HATEOAS's
 * {@code HalFormsConfiguration} — see that class for why this happens at the configuration level
 * rather than via the request-scoped mechanism in {@link HalFormsSupport}.
 */
final class EnumOptionsSupport {

    /** Generous bound on wrapper nesting depth (e.g. {@code JsonNullable<Set<E>>}); real DTOs never nest this deep. */
    private static final int MAX_UNWRAP_DEPTH = 5;

    private EnumOptionsSupport() {
    }

    /**
     * Returns every enum-typed record component of {@code payloadType}, mapped to its constants'
     * serialized values (declaration order, null-valued constants skipped). Components that are not
     * enum-typed (even after unwrapping) or whose enum has no non-null constants are omitted.
     */
    static Map<String, List<String>> enumOptionsByProperty(Class<?> payloadType) {
        if (payloadType == null || !payloadType.isRecord()) {
            return Map.of();
        }
        Map<String, List<String>> result = new java.util.LinkedHashMap<>();
        for (RecordComponent component : payloadType.getRecordComponents()) {
            Class<?> enumType = unwrapEnumType(component.getGenericType());
            if (enumType == null) {
                continue;
            }
            List<String> values = enumConstantValues(enumType);
            if (!values.isEmpty()) {
                result.put(component.getName(), values);
            }
        }
        return result;
    }

    /**
     * Unwraps {@code JsonNullable}/{@code Optional}/{@code Collection} layers (in any nesting, e.g.
     * {@code JsonNullable<Set<E>>}) to find the underlying enum type, or returns null if the
     * property is not enum-typed once unwrapped.
     */
    static Class<?> unwrapEnumType(Type type) {
        Type current = type;
        for (int i = 0; i < MAX_UNWRAP_DEPTH && current instanceof ParameterizedType parameterized; i++) {
            if (!(parameterized.getRawType() instanceof Class<?> rawType)) {
                return null;
            }
            boolean unwrappable = JsonNullable.class.isAssignableFrom(rawType)
                    || Optional.class.isAssignableFrom(rawType)
                    || (Collection.class.isAssignableFrom(rawType) && !Map.class.isAssignableFrom(rawType));
            if (!unwrappable) {
                return null;
            }
            current = parameterized.getActualTypeArguments()[0];
        }
        return (current instanceof Class<?> clazz && clazz.isEnum()) ? clazz : null;
    }

    /**
     * Returns the enum constants' serialized JSON values, in declaration order, using the
     * {@code @JsonValue}-annotated accessor if present (generated DTO enums serialize this way),
     * falling back to {@code name()} otherwise. Constants whose serialized value is null (e.g. a
     * nullable inline enum's placeholder "unset" constant) are skipped.
     */
    static List<String> enumConstantValues(Class<?> enumType) {
        Method jsonValueMethod = findJsonValueMethod(enumType);
        List<String> values = new ArrayList<>();
        for (Object constant : enumType.getEnumConstants()) {
            String value = jsonValueMethod != null
                    ? invokeJsonValue(jsonValueMethod, constant)
                    : ((Enum<?>) constant).name();
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private static Method findJsonValueMethod(Class<?> enumType) {
        for (Method candidate : enumType.getDeclaredMethods()) {
            if (candidate.getParameterCount() == 0 && candidate.isAnnotationPresent(JsonValue.class)) {
                candidate.setAccessible(true);
                return candidate;
            }
        }
        return null;
    }

    private static String invokeJsonValue(Method method, Object constant) {
        try {
            Object result = method.invoke(constant);
            return result != null ? result.toString() : null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read @JsonValue for enum constant " + constant, e);
        }
    }
}
