package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.Event;

import java.time.format.DateTimeFormatter;

final class SyncEntityLabels {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d. M. yyyy");

    private SyncEntityLabels() {
    }

    static String event(Event event) {
        return event.getName() + " (" + event.getEventDate().format(DATE) + ")";
    }
}
