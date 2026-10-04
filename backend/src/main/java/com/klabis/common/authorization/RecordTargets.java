package com.klabis.common.authorization;

import com.klabis.common.security.fieldsecurity.OwnerVisible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the targets a response record is about: the component marked {@link TargetId} or, when the record has
 * {@link OwnerVisible} components, its single component convertible to a UUID (taken to be a member).
 * A component holding several ids yields several targets.
 */
final class RecordTargets {

    private static final Logger LOG = LoggerFactory.getLogger(RecordTargets.class);

    private final UuidConversion uuids;
    private final ConcurrentHashMap<Class<?>, Optional<RecordTarget>> cache = new ConcurrentHashMap<>();

    RecordTargets(UuidConversion uuids) {
        this.uuids = uuids;
    }

    List<TargetRef> of(Object record) {
        Optional<RecordTarget> recordTarget = cache.computeIfAbsent(record.getClass(), this::discover);
        if (recordTarget.isEmpty()) {
            return List.of();
        }
        RecordTarget target = recordTarget.get();
        Object value;
        try {
            value = target.accessor().invoke(record);
        } catch (ReflectiveOperationException e) {
            LOG.warn("Failed to read target id from {}", record.getClass().getSimpleName(), e);
            return List.of();
        }
        Collection<?> values = value instanceof Collection<?> collection ? collection : value == null ? List.of() : List.of(value);
        return values.stream()
                .map(uuids::toUuid)
                .filter(Objects::nonNull)
                .map(id -> new TargetRef(target.type(), id))
                .toList();
    }

    private Optional<RecordTarget> discover(Class<?> recordClass) {
        RecordComponent[] components = recordClass.getRecordComponents();
        if (components == null) {
            return Optional.empty();
        }
        for (RecordComponent component : components) {
            TargetId targetId = component.getAccessor().getAnnotation(TargetId.class);
            if (targetId != null) {
                return Optional.of(RecordTarget.of(component.getAccessor(), targetId.value()));
            }
        }
        return discoverOwnerTarget(recordClass, components);
    }

    private Optional<RecordTarget> discoverOwnerTarget(Class<?> recordClass, RecordComponent[] components) {
        boolean anyOwnerVisible = Arrays.stream(components)
                .anyMatch(component -> component.getAccessor().getAnnotation(OwnerVisible.class) != null);
        if (!anyOwnerVisible) {
            return Optional.empty();
        }
        List<RecordComponent> candidates = Arrays.stream(components)
                .filter(component -> uuids.canConvert(component.getType()))
                .toList();
        if (candidates.size() == 1) {
            return Optional.of(RecordTarget.of(candidates.get(0).getAccessor(), TargetType.MEMBER));
        }
        LOG.warn("Cannot resolve owner id of {}: found {} UUID-convertible components. Annotate the owner component with @TargetId.",
                recordClass.getSimpleName(), candidates.size());
        return Optional.empty();
    }

    private record RecordTarget(Method accessor, TargetType type) {

        static RecordTarget of(Method accessor, TargetType type) {
            accessor.trySetAccessible();
            return new RecordTarget(accessor, type);
        }
    }
}
