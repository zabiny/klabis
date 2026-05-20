package com.klabis.calendar.infrastructure.ical;

import com.klabis.calendar.application.IcalFeedPort.EventScheduleEntry;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventStatus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serializes events to iCalendar (RFC 5545) format.
 * Manual serialization — no third-party library dependency.
 * Output uses CRLF line endings and UTF-8 encoding per RFC 5545.
 */
public class ICalendarRenderer {

    private static final DateTimeFormatter DTSTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final String CRLF = "\r\n";

    /**
     * Renders the iCalendar feed.
     *
     * @param entries  events to include, each paired with the member's role
     * @param baseUrl  base URL of the Klabis application (e.g. {@code https://klabis.example.com})
     *                 used to build event detail links
     * @param dtstamp  the current instant used as DTSTAMP for all VEVENTs
     * @return iCalendar text (UTF-8, CRLF line endings)
     */
    public String render(List<EventScheduleEntry> entries, String baseUrl, Instant dtstamp) {
        StringBuilder sb = new StringBuilder();

        sb.append("BEGIN:VCALENDAR").append(CRLF);
        sb.append("VERSION:2.0").append(CRLF);
        sb.append("PRODID:-//Klabis//Klabis Member Portal//CS").append(CRLF);

        for (EventScheduleEntry entry : entries) {
            appendVEvent(sb, entry.event(), entry.isCoordinator(), baseUrl, dtstamp);
        }

        sb.append("END:VCALENDAR").append(CRLF);

        return sb.toString();
    }

    private void appendVEvent(StringBuilder sb, Event event, boolean isCoordinator, String baseUrl, Instant dtstamp) {
        String eventDetailUrl = baseUrl + "/events/" + event.getId().value();
        String dtstart = DATE_FORMAT.format(event.getEventDate());
        String dtend = DATE_FORMAT.format(event.getEventDate().plusDays(1));
        String uid = event.getId().value() + "@klabis";

        sb.append("BEGIN:VEVENT").append(CRLF);
        sb.append("UID:").append(uid).append(CRLF);
        sb.append("DTSTAMP:").append(DTSTAMP_FORMAT.format(dtstamp)).append(CRLF);
        sb.append("DTSTART;VALUE=DATE:").append(dtstart).append(CRLF);
        sb.append("DTEND;VALUE=DATE:").append(dtend).append(CRLF);
        sb.append("SUMMARY:").append(escapeText(event.getName())).append(CRLF);

        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            sb.append("LOCATION:").append(escapeText(event.getLocation())).append(CRLF);
        }

        sb.append("URL:").append(eventDetailUrl).append(CRLF);

        String description = buildDescription(event, eventDetailUrl, isCoordinator);
        sb.append("DESCRIPTION:").append(escapeText(description)).append(CRLF);

        if (event.getStatus() == EventStatus.CANCELLED) {
            sb.append("STATUS:CANCELLED").append(CRLF);
        } else {
            sb.append("STATUS:CONFIRMED").append(CRLF);
        }

        sb.append("END:VEVENT").append(CRLF);
    }

    private String buildDescription(Event event, String eventDetailUrl, boolean isCoordinator) {
        StringBuilder desc = new StringBuilder();
        desc.append("Pořadatel: ").append(event.getOrganizer());
        desc.append("\n\nDetail v Klabisu: ").append(eventDetailUrl);

        if (event.getWebsiteUrl() != null) {
            desc.append("\nWeb akce: ").append(event.getWebsiteUrl().value());
        }

        if (isCoordinator) {
            desc.append("\nRole: koordinátor");
        }

        return desc.toString();
    }

    /**
     * Escapes text values per RFC 5545 §3.3.11:
     * backslash → \\, comma → \,, semicolon → \;, newline → \n
     */
    static String escapeText(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }
}
