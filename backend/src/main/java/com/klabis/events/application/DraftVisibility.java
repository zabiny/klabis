package com.klabis.events.application;

import com.klabis.events.EventId;

import java.util.Set;

public sealed interface DraftVisibility {

    static DraftVisibility all() {
        return All.INSTANCE;
    }

    static DraftVisibility only(Set<EventId> eventIds) {
        return new Only(Set.copyOf(eventIds));
    }

    enum All implements DraftVisibility {
        INSTANCE
    }

    record Only(Set<EventId> eventIds) implements DraftVisibility {
    }
}
