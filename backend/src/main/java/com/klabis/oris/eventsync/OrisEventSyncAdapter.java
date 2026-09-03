package com.klabis.oris.eventsync;

import com.klabis.events.EventId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.OrisEventFields;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.oris.OrisIntegrationComponent;
import com.klabis.sync.domain.*;
import org.jmolecules.architecture.hexagonal.Application;

import java.util.Optional;
import java.util.UUID;

/**
 * The ORIS event {@link SynchronizationAdapter} (design.md D2, D3): inward-only,
 * reusing the ORIS field mapping already used for manual import and sync
 * ({@link OrisEventImportPort}).
 * <p>
 * Reaches the {@code events} module only through its {@code events.application}
 * primary ports — {@link EventManagementPort} to read the local side,
 * {@link OrisEventImportPort} to read the external side and to write inward via
 * {@code Event.syncFromOris} — so this module gains no knowledge of {@code events}
 * internals beyond what {@code OrisController} already has (design.md D2).
 * <p>
 * Declares no outward write, no create on either side, and no sensitive data: ORIS
 * event data is public, and Klabis has no way to push event changes back to ORIS.
 * A local edit to an ORIS-owned field therefore always surfaces as a conflict rather
 * than being silently overwritten or silently sent onward (design.md D6).
 * <p>
 * Classified as {@link Application}, not as a hexagonal adapter: this class holds two
 * hexagonal roles at once — a driven adapter implementing {@code sync}'s
 * {@link SynchronizationAdapter} secondary port, and a driving adapter calling
 * {@code events}' {@code @PrimaryPort} interfaces above. jMolecules cannot express
 * both on one class, since {@code @PrimaryAdapter} and {@code @SecondaryAdapter} are
 * mutually exclusive in the library's own layer predicates, and a
 * {@code @SecondaryAdapter} may never reach a primary port. {@code Application} is the
 * classification that permits the primary-port access D2 prescribes. The D2 dependency
 * direction is unchanged and still correct.
 */
@OrisIntegrationComponent
@Application
class OrisEventSyncAdapter implements SynchronizationAdapter {

    private static final SyncCapabilities CAPABILITIES =
            new SyncCapabilities(true, true, true, false, false, false, false);

    private final EventManagementPort eventManagementPort;
    private final OrisEventImportPort orisEventImportPort;

    OrisEventSyncAdapter(EventManagementPort eventManagementPort, OrisEventImportPort orisEventImportPort) {
        this.eventManagementPort = eventManagementPort;
        this.orisEventImportPort = orisEventImportPort;
    }

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.EVENT;
    }

    @Override
    public ExternalSystem system() {
        return ExternalSystem.ORIS;
    }

    @Override
    public SyncCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public Class<? extends SyncProjection> projectionType() {
        return OrisEventProjection.class;
    }

    @Override
    public SyncProjection readLocal(String entityId) {
        Event event = eventManagementPort.getEvent(toEventId(entityId), true);
        return OrisEventProjectionMapper.fromEvent(event);
    }

    @Override
    public SyncProjection readExternal(String externalId) {
        OrisEventFields fields = orisEventImportPort.readOrisFields(toOrisId(externalId));
        return OrisEventFieldsToProjectionMapper.fromOrisFields(fields);
    }

    /**
     * Always empty: {@code oris-client} offers no cheap per-event or per-list version
     * signal today. {@code getEventList} returns no version field, and
     * {@code EventDetails.version()} is only available after the full read this token
     * exists to avoid — reading it would not save anything. The engine falls back to a
     * full read on every pass, as it does for any adapter without a token
     * (design.md D3).
     */
    @Override
    public Optional<ExternalVersionToken> externalVersion(String externalId) {
        return Optional.empty();
    }

    @Override
    public void applyToLocal(String entityId, SyncProjection projection) {
        EventId eventId = toEventId(entityId);
        OrisEventProjection orisProjection = (OrisEventProjection) projection;
        OrisEventFields fields = withResolvedEventType(eventId, OrisEventProjectionToFieldsMapper.toOrisEventFields(orisProjection));
        orisEventImportPort.applyOrisSync(eventId, fields);
    }

    @Override
    public void applyToExternal(String externalId, SyncProjection projection) {
        throw new UnsupportedOperationException(
                "The ORIS event adapter declares no outward write capability");
    }

    /**
     * The event type is Klabis-owned (design.md D3) and therefore absent from the
     * projection, but {@code Event.applyAutoMappedEventType} still needs the ORIS
     * discipline resolution to preserve today's auto-mapping behaviour on a write
     * that came through the engine (task 7.5). Re-resolved from the event's own
     * {@code orisId} rather than threaded through the projection, so the projection
     * stays a plain carrier of comparable fields only.
     */
    private OrisEventFields withResolvedEventType(EventId eventId, OrisEventFields fields) {
        Event event = eventManagementPort.getEvent(eventId, true);
        Integer orisId = event.getOrisId();
        if (orisId == null) {
            return fields;
        }
        var resolvedEventTypeId = orisEventImportPort.readOrisFields(orisId).resolvedEventTypeId();
        return new OrisEventFields(
                fields.name(), fields.eventDate(), fields.location(), fields.organizer(),
                fields.websiteUrl(), fields.registrationDeadlines(), fields.categories(),
                fields.ranking(), fields.baseEntryFee(), resolvedEventTypeId
        );
    }

    private static EventId toEventId(String entityId) {
        return new EventId(UUID.fromString(entityId));
    }

    private static int toOrisId(String externalId) {
        return Integer.parseInt(externalId);
    }
}
