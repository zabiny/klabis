package club.klabis.oris.application;

import club.klabis.events.domain.OrisId;
import club.klabis.oris.application.dto.*;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Collection;
import java.util.Optional;

@SecondaryPort
public interface OrisDataProvider {

    Optional<OrisUserInfo> getUserInfo(String registrationNumber);

    Collection<EventSummary> getEventList(OrisEventListFilter filter);

    Optional<EventDetails> getEventDetails(OrisId eventId);

    Collection<EventEntry> getZbmEventEntries(int eventId);
}
