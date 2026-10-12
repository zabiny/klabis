package com.klabis.common.ui;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.RepresentationModelProcessorInvoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs postprocessors on {@link RepresentationModel} values nested inside a payload record
 * (e.g. {@code List<EntityModel<OwnerResponse>>} in a group detail), so per-item postprocessors
 * registered by item DTO type apply the same way they do for list endpoints.
 * <p>
 * Only payload containers are traversed: records and collections. The root payload itself is
 * never processed here (the advice already did that), and each model instance is processed at
 * most once per walk, which also stops cycles. A model's own content is walked after the model
 * itself has been processed, so nested items may in turn carry nested models.
 * <p>
 * The invoker's return value is discarded: items live in immutable records, so a processor must
 * mutate and return the model it was given (the contract all current postprocessors follow).
 */
final class NestedModelProcessing {

    private static final Map<Class<?>, Method[]> ACCESSORS = new ConcurrentHashMap<>();

    private final RepresentationModelProcessorInvoker invoker;

    NestedModelProcessing(RepresentationModelProcessorInvoker invoker) {
        this.invoker = invoker;
    }

    void process(Object payload) {
        walk(payload, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private void walk(Object node, Set<Object> visited) {
        if (!isContainer(node) || !visited.add(node)) {
            return;
        }
        if (node instanceof RepresentationModel<?> model) {
            invokeFor(model);
            walkModelContent(model, visited);
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(element -> walk(element, visited));
        } else {
            for (Method accessor : accessorsOf(node.getClass())) {
                walk(read(accessor, node), visited);
            }
        }
    }

    private static boolean isContainer(Object node) {
        if (node == null) {
            return false;
        }
        return node instanceof RepresentationModel<?>
               || node instanceof Collection<?>
               || (node.getClass().isRecord() && !node.getClass().getName().startsWith("java."));
    }

    private static Method[] accessorsOf(Class<?> recordType) {
        return ACCESSORS.computeIfAbsent(recordType, type -> {
            Method[] accessors = Arrays.stream(type.getRecordComponents())
                    .map(RecordComponent::getAccessor)
                    .toArray(Method[]::new);
            for (Method accessor : accessors) {
                accessor.setAccessible(true);
            }
            return accessors;
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void invokeFor(RepresentationModel<?> model) {
        invoker.invokeProcessorsFor((RepresentationModel) model);
    }

    private void walkModelContent(RepresentationModel<?> model, Set<Object> visited) {
        if (model instanceof EntityModel<?> entityModel) {
            walk(entityModel.getContent(), visited);
        }
    }

    private static Object read(Method accessor, Object record) {
        try {
            return accessor.invoke(record);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read record component " + accessor, e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Record component accessor failed: " + accessor, e.getCause());
        }
    }
}
