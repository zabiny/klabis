package com.dpolach.eventsourcing;

public interface EventSourcedAggregate {

    void apply(BaseEvent event);

}
