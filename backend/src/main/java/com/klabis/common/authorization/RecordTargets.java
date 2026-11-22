package com.klabis.common.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the target a response record is about: the component marked {@link TargetId}.
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
        UUID id = uuids.toUuid(value);
        return id != null ? List.of(new TargetRef(target.type(), id)) : List.of();
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
        return Optional.empty();
    }

    private record RecordTarget(Method accessor, TargetType type) {

        static RecordTarget of(Method accessor, TargetType type) {
            accessor.trySetAccessible();
            return new RecordTarget(accessor, type);
        }
    }
}
