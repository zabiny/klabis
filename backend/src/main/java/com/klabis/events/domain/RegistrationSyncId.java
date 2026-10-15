package com.klabis.events.domain;

import com.klabis.events.EventId;
import com.klabis.members.MemberId;

import java.util.UUID;

/**
 * Identity of a registration in the synchronisation engine: {@code <eventId>:<memberId>}. A registration is not an
 * aggregate of its own, so it has no id of its own; the engine's entity id is composed from its two owners.
 */
public record RegistrationSyncId(EventId eventId, MemberId memberId) {

    private static final char SEPARATOR = ':';

    public RegistrationSyncId {
        if (eventId == null || memberId == null) {
            throw new IllegalArgumentException("Both event id and member id are required");
        }
    }

    public static RegistrationSyncId parse(String value) {
        int separator = value == null ? -1 : value.indexOf(SEPARATOR);
        if (separator < 0) {
            throw new IllegalArgumentException("Not a registration sync id: '" + value + "'");
        }
        try {
            return new RegistrationSyncId(
                    new EventId(UUID.fromString(value.substring(0, separator))),
                    new MemberId(UUID.fromString(value.substring(separator + 1))));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Not a registration sync id: '" + value + "'", e);
        }
    }

    /**
     * The leading part shared by the ids of every registration of the event.
     */
    public static String prefixOf(EventId eventId) {
        return eventId.value().toString() + SEPARATOR;
    }

    public String format() {
        return prefixOf(eventId) + memberId.value();
    }
}
