package com.klabis.events.application;

import com.klabis.events.EventId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.NonNull;

@PrimaryPort
public interface AccommodationListPort {

    AccommodationList getAccommodationList(@NonNull EventId eventId);
}
