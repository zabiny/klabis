package club.klabis.oris.infrastructure.apiclient;

import club.klabis.events.domain.OrisId;
import club.klabis.oris.application.OrisDataProvider;
import club.klabis.oris.application.dto.*;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collection;
import java.util.Optional;

@SecondaryAdapter
@Service
class OrisApiDataProvider implements OrisDataProvider {
    private final OrisApiClient apiClient;

    public OrisApiDataProvider(OrisApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Optional<OrisUserInfo> getUserInfo(String registrationNumber) {
        try {
            return Optional.ofNullable(apiClient.getUserInfo(registrationNumber).data());
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }
    }

    @Override
    public Collection<EventSummary> getEventList(OrisEventListFilter filter) {
        return apiClient.getEventList(filter).data().values();
    }

    @Override
    public Optional<EventDetails> getEventDetails(OrisId eventId) {
        try {
            return Optional.ofNullable(apiClient.getEventDetails(eventId.value()).data());
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }
    }

    @Override
    public Collection<EventEntry> getZbmEventEntries(int eventId) {
        return apiClient.getEventEntries(eventId, OrisApiClient.CLUB_ID_ZBM).data().values();
    }
}
