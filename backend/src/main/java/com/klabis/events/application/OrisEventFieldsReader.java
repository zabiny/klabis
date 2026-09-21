package com.klabis.events.application;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.Discipline;
import com.dpolach.api.orisclient.dto.EventDetails;
import com.klabis.events.DisciplineId;
import com.klabis.events.EventTypeId;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeRepository;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncedEntityReference;
import org.springframework.context.annotation.Lazy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
 * not happening. The local reads triggered by {@code resolveEventTypeFromOrisDiscipline}
 * ({@link SynchronizationPort#findByExternalReferences} and {@code eventTypeRepository
 * .findByDisciplineId}) run fine without an explicit read-only transaction (design.md D6).
 * <p>
 * {@link SynchronizationPort} is injected {@code @Lazy}: its implementation depends on
 * {@code SynchronizationAdapterRegistry}, which eagerly collects every
 * {@code SynchronizationAdapter} bean — including {@code OrisEventSyncAdapter}, which
 * itself depends on this class. Without {@code @Lazy}, that forms a circular bean
 * dependency ({@code OrisEventFieldsReader -> SynchronizationPort ->
 * SynchronizationAdapterRegistry -> OrisEventSyncAdapter -> OrisEventFieldsReader}) that
 * Spring cannot resolve at startup.
 */
@OrisIntegrationComponent
public class OrisEventFieldsReader {

    private final OrisApiClient orisApiClient;
    private final OrisWebUrls orisWebUrls;
    private final EventTypeRepository eventTypeRepository;
    private final SynchronizationPort synchronizationPort;

    OrisEventFieldsReader(OrisApiClient orisApiClient,
                          OrisWebUrls orisWebUrls,
                          EventTypeRepository eventTypeRepository,
                          @Lazy SynchronizationPort synchronizationPort) {
        this.orisApiClient = orisApiClient;
        this.orisWebUrls = orisWebUrls;
        this.eventTypeRepository = eventTypeRepository;
        this.synchronizationPort = synchronizationPort;
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
        return findPairedDisciplineId(discipline.id())
                .flatMap(eventTypeRepository::findByDisciplineId)
                .map(EventType::getId)
                .orElse(null);
    }

    private Optional<DisciplineId> findPairedDisciplineId(int orisDisciplineId) {
        List<SyncedEntityReference> pairings = synchronizationPort.findByExternalReferences(
                SyncEntityType.DISCIPLINE, ExternalSystem.ORIS, List.of(String.valueOf(orisDisciplineId)));
        return pairings.stream()
                .findFirst()
                .map(reference -> new DisciplineId(UUID.fromString(reference.target().entityId())));
    }
}
