package com.dpolach.eventsourcing;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.context.event.EventListener;

import java.util.Optional;

public interface Projector<T> {

    @EventListener
    default void project(BaseEvent event) {
        String sourceModule = BaseEvent.getModuleName(getClass());
        if (sourceModule.equals(event.getModuleName())) {
            throw new NotImplementedException(
                    "Projector %s doesn't handle event %s from same module (projectors must apply all events from same module)".formatted(
                            getClass().getCanonicalName(), event.getClass().getCanonicalName()));
        }
    }

    Optional<T> getResult();

    // internal use - called once all pending events were projected (needed for EventSourced aggregate roots to clear pending events)
    default void completed() {
    }

}
