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
 * This module depends on the events module for:
 * <ul>
 *   <li>Domain events (EventPublishedEvent, EventUpdatedEvent, EventCancelledEvent)</li>
 *   <li>Cross-module query port (EventScheduleQuery)</li>
 *   <li>Event aggregate read access (Events, Event) via the events.domain named interface</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(displayName = "Kalendář")
package com.klabis.calendar;
