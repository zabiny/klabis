/**
 * ORIS integration: a passthrough onto the external ORIS catalogue, exposed by
 * {@link com.klabis.oris.OrisController} at {@code /api/oris/events} for the UI's
 * "Import from ORIS" dialog. These are not Klabis resources.
 * <p>
 * Its only dependency into {@code events} is
 * {@link com.klabis.events.application.ImportedOrisEventsPort}, used solely to filter
 * out events already imported. The ORIS synchronisation adapter is not here: it lives in
 * {@code com.klabis.events.infrastructure.orissync} and implements {@code sync}'s
 * {@link com.klabis.sync.domain.SynchronizationAdapter} secondary port as a module-internal
 * collaborator of {@code events}, so it never crosses this boundary.
 * <p>
 * Declaring this package a module makes that single edge build-enforced rather than
 * conventional (openspec {@code relocate-oris-event-sync-adapter}, design.md D6). The four
 * {@code oris → events.domain} imports this change removed would have failed the build had
 * the declaration existed at the time.
 */
@org.springframework.modulith.ApplicationModule(displayName = "ORIS")
package com.klabis.oris;
