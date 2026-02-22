/**
 * Event-driven integration feature for synchronizing calendar items with events.
 * <p>
 * This feature handles cross-module integration between calendar and events modules
 * using domain events and hexagonal architecture patterns.
 * <p>
 * <b>Components:</b>
 * <ul>
 *   <li>{@link com.klabis.calendar.application.EventsEventListener} - Primary adapter (event listener)</li>
 *   <li>{@link com.klabis.calendar.application.CalendarEventSyncPort} - Primary port (business interface)</li>
 *   <li>{@link com.klabis.calendar.application.CalendarEventSyncService} - Application service</li>
 *   <li>{@link com.klabis.calendar.application.EventData} - DTO for event data</li>
 * </ul>
 */
@NamedInterface("eventsintegration")
package com.klabis.calendar.application;

import org.springframework.modulith.NamedInterface;
