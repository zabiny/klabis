package com.dpolach.eventsourcing;

import org.springframework.context.event.EventListener;

public interface Projector<T> {

    @EventListener
    void project(BaseEvent event);

}
