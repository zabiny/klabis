package com.klabis.events.application;

import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface OrisEventRegistrationDiscoveryPort {

    /**
     * Reads the club's ORIS entries of the event afresh and brings in entries of members that are paired with their
     * ORIS club membership and not yet paired as registrations. Does nothing for an event that is not ACTIVE or not
     * kept in step with ORIS.
     *
     * @param actingUser the user who asked for the discovery, recorded by the synchronisation engine
     * @return the number of registrations brought in
     */
    int discoverForEvent(EventId eventId, String actingUser);
}
