package com.klabis.oris.eventsync;

import com.klabis.events.EventId;
import com.klabis.events.application.EventManagementPort;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.oris.OrisIntegrationComponent;
import com.klabis.sync.domain.*;
import org.jmolecules.architecture.hexagonal.Application;
import org.springframework.context.annotation.Lazy;

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
 * {@link #readExternal} resolves the ORIS discipline mapping once and carries it on
 * the returned {@link OrisEventProjection#resolvedEventTypeId()} itself, so
 * {@link #applyToLocal} reads it straight off the projection it was given rather than
 * from separate call-order-dependent state — no second ORIS read (task 8.10), and no
 * risk of one record's resolution leaking onto another's write.
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
 * <p>
 * {@code orisEventImportPort} is {@link Lazy} because task 8.3 (delegating
 * {@code syncEventFromOris} to the engine) closes a bean-construction cycle that D2's
 * dependency direction always implied but never triggered until now:
 * {@code OrisEventImportService} (implements {@link OrisEventImportPort}) now needs
 * {@code SynchronizationPort}, which needs {@link SynchronizationAdapterRegistry},
 * which needs this adapter, which needs {@code OrisEventImportPort} back.
 * {@code eventManagementPort} does not participate in the cycle and stays eager.
 */
@OrisIntegrationComponent
@Application
class OrisEventSyncAdapter implements SynchronizationAdapter {

    private static final SyncCapabilities CAPABILITIES =
            new SyncCapabilities(true, true, true, false, false, false, false);

    private final EventManagementPort eventManagementPort;
    private final OrisEventImportPort orisEventImportPort;

    OrisEventSyncAdapter(EventManagementPort eventManagementPort, @Lazy OrisEventImportPort orisEventImportPort) {
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
        return OrisEventFieldsToProjectionMapper.fromOrisFields(
                orisEventImportPort.readOrisFields(toOrisId(externalId)));
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
        orisEventImportPort.applyOrisSync(eventId, OrisEventProjectionToFieldsMapper.toOrisEventFields(orisProjection));
    }

    @Override
    public void applyToExternal(String externalId, SyncProjection projection) {
        throw new UnsupportedOperationException(
                "The ORIS event adapter declares no outward write capability");
    }

    private static EventId toEventId(String entityId) {
        return new EventId(UUID.fromString(entityId));
    }

    private static int toOrisId(String externalId) {
        return Integer.parseInt(externalId);
    }
}
