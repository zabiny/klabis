package com.klabis.common.ui;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.mvc.RepresentationModelProcessorInvoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

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

    private final RepresentationModelProcessorInvoker invoker;

    NestedModelProcessing(RepresentationModelProcessorInvoker invoker) {
        this.invoker = invoker;
    }

    void process(Object payload) {
        walk(payload, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private void walk(Object node, Set<Object> visited) {
        if (node == null || !visited.add(node)) {
            return;
        }
        if (node instanceof RepresentationModel<?> model) {
            invokeFor(model);
            walkModelContent(model, visited);
        } else if (node instanceof Collection<?> collection) {
            collection.forEach(element -> walk(element, visited));
        } else if (node.getClass().isRecord()) {
            for (RecordComponent component : node.getClass().getRecordComponents()) {
                walk(read(component, node), visited);
            }
        }
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

    private static Object read(RecordComponent component, Object record) {
        Method accessor = component.getAccessor();
        try {
            accessor.setAccessible(true);
            return accessor.invoke(record);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read record component " + component, e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Record component accessor failed: " + component, e.getCause());
        }
    }
}
