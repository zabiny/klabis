package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
import com.klabis.common.OrisIntegrationComponent;

/**
 * Fetches an ORIS event's details and maps them to {@link OrisEventFields}, shared by
 * {@link OrisEventImportService} (import) and by
 * {@code com.klabis.events.infrastructure.orissync.OrisEventSyncAdapter} (the
 * synchronisation engine's external-side read), so the {@code EventDetails} mapping
 * exists exactly once (design.md D4, open question 1).
 * <p>
 * Deliberately NOT {@code @Transactional}: its only caller is {@code
 * OrisEventSyncAdapter.readExternal}, which the synchronisation engine always invokes
 * through {@code ResilientAdapterExecutor} with no transaction open (design.md D8,
 * D12) — wrapping this method would open one around the blocking
 * {@code orisApiClient.getEventDetails} call, the exact thing that design relies on
 * not happening. The single local read ({@code eventTypeRepository
 * .findByOrisDisciplineId}) runs fine without an explicit read-only transaction.
 */
@OrisIntegrationComponent
public class OrisEventFieldsReader {

    private final OrisApiClient orisApiClient;
    private final OrisWebUrls orisWebUrls;
    private final EventTypeRepository eventTypeRepository;

    OrisEventFieldsReader(OrisApiClient orisApiClient,
                          OrisWebUrls orisWebUrls,
                          EventTypeRepository eventTypeRepository) {
        this.orisApiClient = orisApiClient;
        this.orisWebUrls = orisWebUrls;
        this.eventTypeRepository = eventTypeRepository;
    }

    public OrisEventFields readOrisFields(int orisId) {
        EventDetails details = fetchEventDetails(orisId);
        EventTypeId resolvedEventTypeId = resolveEventTypeFromOrisDiscipline(details.discipline());
        return OrisEventDetailsMapper.map(details, orisId, orisWebUrls, resolvedEventTypeId);
    }

    private EventDetails fetchEventDetails(int orisId) {
        return orisApiClient.getEventDetails(orisId).payload()
                .orElseThrow(() -> new EventNotFoundException(orisId));
    }

    private EventTypeId resolveEventTypeFromOrisDiscipline(Discipline discipline) {
        if (discipline == null || discipline.id() <= 0) {
            // ORIS uses id 0 as sentinel for a missing discipline
            return null;
        }
        return eventTypeRepository.findByOrisDisciplineId(discipline.id())
                .map(EventType::getId)
                .orElse(null);
    }
}
