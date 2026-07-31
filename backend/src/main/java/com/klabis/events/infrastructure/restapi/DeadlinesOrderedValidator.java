package com.klabis.events.infrastructure.restapi;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.openapitools.jackson.nullable.JsonNullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.List;

/**
 * Reads {@code deadlines} reflectively because the create and update requests are unrelated
 * generated types, and the update request additionally wraps the list in {@link JsonNullable}.
 *
 * <p>The violation is re-anchored on the {@code deadlines} property so the 400 response keeps
 * naming the offending field, as it did when this was an {@code @AssertTrue} accessor.
 *
 * <p>Public because Hibernate Validator's default factory instantiates validators reflectively and
 * rejects a package-private class with HV000064.
 */
public class DeadlinesOrderedValidator implements ConstraintValidator<DeadlinesOrdered, Object> {

    private static final String PROPERTY = "deadlines";

    @Override
    public boolean isValid(Object request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        if (isOrdered(readDeadlines(request))) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(PROPERTY)
                .addConstraintViolation();
        return false;
    }

    @SuppressWarnings("unchecked")
    private static List<LocalDate> readDeadlines(Object request) {
        for (RecordComponent component : request.getClass().getRecordComponents()) {
            if (!PROPERTY.equals(component.getName())) {
                continue;
            }
            Object value;
            try {
                value = component.getAccessor().invoke(request);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException(
                        "Cannot read " + PROPERTY + " of " + request.getClass().getName(), e);
            }
            if (value instanceof JsonNullable<?> wrapper) {
                value = wrapper.isPresent() ? wrapper.orElseThrow() : null;
            }
            return (List<LocalDate>) value;
        }
        throw new IllegalStateException(
                "@DeadlinesOrdered requires a " + PROPERTY + " record component, but "
                + request.getClass().getName() + " has none");
    }

    private static boolean isOrdered(List<LocalDate> deadlines) {
        if (deadlines == null || deadlines.size() < 2) {
            return true;
        }
        for (int i = 1; i < deadlines.size(); i++) {
            LocalDate previous = deadlines.get(i - 1);
            LocalDate current = deadlines.get(i);
            // The spec declares the items non-nullable, so Jackson cannot produce a null element
            // here. Skipping rather than dereferencing keeps a hand-constructed list from turning
            // this constraint into an NPE — reporting "unordered" for a null would be a lie.
            if (previous == null || current == null) {
                continue;
            }
            if (current.isBefore(previous)) {
                return false;
            }
        }
        return true;
    }
}
