package com.dpolach.eventsourcing;

import org.springframework.data.annotation.Id;

import java.time.ZonedDateTime;
import java.util.Comparator;

// TODO: do we want this as base class or as wrapper (= data of event as 'payload' attribute in this object containing other metadata)
public class BaseEvent implements Comparable<BaseEvent> {
    private static long maxSequence = 0l;

    @Id
    private final long sequenceId;
    private final ZonedDateTime createdAt;
    private final String moduleName;
    private final String sourceAggregateType;

    public BaseEvent(Class<?> sourceAggregateType) {
        this.sequenceId = maxSequence++;
        this.createdAt = ZonedDateTime.now();
        this.moduleName = getModuleName(getClass());
        this.sourceAggregateType = sourceAggregateType.getCanonicalName();
    }

    public BaseEvent(Class<?> sourceAggregateType, long sequenceId, ZonedDateTime createdAt) {
        this.sequenceId = sequenceId;
        this.createdAt = createdAt;
        this.moduleName = getModuleName(getClass());
        this.sourceAggregateType = sourceAggregateType.getCanonicalName();
    }

    static String getModuleName(Class<?> type) {
        return type.getPackage().getName().replaceFirst("club\\.klabis\\.", "").split("\\.", 2)[0];
    }

    public Class<?> getAggregateType() throws ClassNotFoundException {
        return Class.forName(sourceAggregateType);
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public long getSequenceId() {
        return sequenceId;
    }

    public String getModuleName() {
        return moduleName;
    }

    @Override
    public int compareTo(BaseEvent o) {
        return Comparator.comparing(BaseEvent::getSequenceId).compare(this, o);
    }
}
