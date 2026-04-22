/**
 * Calendar bounded context.
 * <p>
 * This module manages calendar items that display date-based events on a calendar view.
 * Calendar items can be:
 * <ul>
 *   <li><b>Event-linked:</b> Auto-generated from Events (via EventPublishedEvent) and kept in sync (via EventUpdatedEvent/EventCancelledEvent)</li>
 *   <li><b>Manual:</b> Created by authorized users (CALENDAR:MANAGE) for club activities, reminders, and deadlines</li>
 * </ul>
 * <p>
 * Event-linked items are read-only; only manual items can be edited or deleted.
 * <p>
 * This module consumes domain events from the Events module but does not depend on it directly,
 * following Spring Modulith's event-driven architecture.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Kalendář")
package com.klabis.calendar;
