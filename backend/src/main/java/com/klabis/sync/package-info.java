/**
 * The synchronisation engine: one mechanism for change detection, conflict handling,
 * retry and audit, usable by any Klabis entity and any external system.
 * <p>
 * This module is unaware of any concrete external system — no type here names ORIS.
 * Integrations (e.g. {@code com.klabis.oris.sync}) implement the
 * {@link com.klabis.sync.domain.SynchronizationAdapter} secondary port and reach the
 * engine through {@link com.klabis.sync.application}.
 *
 * @see <a href="../../../../../../../../openspec/changes/add-bidirectional-sync-engine/design.md">design.md</a>
 */
@org.springframework.modulith.ApplicationModule(displayName = "Synchronizace")
package com.klabis.sync;
