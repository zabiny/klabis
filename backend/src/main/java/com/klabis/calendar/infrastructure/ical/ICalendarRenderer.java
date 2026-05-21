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

        appendLine(sb, "BEGIN:VCALENDAR");
        appendLine(sb, "VERSION:2.0");
        appendLine(sb, "PRODID:-//Klabis//Klabis Member Portal//CS");

        for (EventScheduleEntry entry : entries) {
            appendVEvent(sb, entry.event(), entry.isCoordinator(), baseUrl, dtstamp);
        }

        appendLine(sb, "END:VCALENDAR");

        return sb.toString();
    }

    private void appendVEvent(StringBuilder sb, Event event, boolean isCoordinator, String baseUrl, Instant dtstamp) {
        String eventDetailUrl = baseUrl + "/events/" + event.getId().value();
        String dtstart = DATE_FORMAT.format(event.getEventDate());
        String dtend = DATE_FORMAT.format(event.getEventDate().plusDays(1));
        String uid = event.getId().value() + "@klabis";

        appendLine(sb, "BEGIN:VEVENT");
        appendLine(sb, "UID:" + uid);
        appendLine(sb, "DTSTAMP:" + DTSTAMP_FORMAT.format(dtstamp));
        appendLine(sb, "DTSTART;VALUE=DATE:" + dtstart);
        appendLine(sb, "DTEND;VALUE=DATE:" + dtend);
        appendLine(sb, "SUMMARY:" + escapeText(event.getName()));

        if (event.getLocation() != null && !event.getLocation().isBlank()) {
            appendLine(sb, "LOCATION:" + escapeText(event.getLocation()));
        }

        appendLine(sb, "URL:" + eventDetailUrl);

        String description = buildDescription(event, eventDetailUrl, isCoordinator);
        appendLine(sb, "DESCRIPTION:" + escapeText(description));

        if (event.getStatus() == EventStatus.CANCELLED) {
            appendLine(sb, "STATUS:CANCELLED");
        } else {
            appendLine(sb, "STATUS:CONFIRMED");
        }

        appendLine(sb, "END:VEVENT");
    }

    /**
     * Appends a single iCalendar content line to the buffer, folding at 75-octet boundaries
     * per RFC 5545 §3.1. Long lines are split with CRLF + single SPACE continuation.
     * Folding is octet-based (UTF-8 bytes) to correctly handle multi-byte characters.
     */
    private void appendLine(StringBuilder sb, String line) {
        byte[] bytes = line.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length <= 75) {
            sb.append(line).append(CRLF);
            return;
        }

        int offset = 0;
        boolean first = true;
        while (offset < bytes.length) {
            int limit = first ? 75 : 74; // first line: 75 octets; continuation: 74 octets (1 used by leading space)
            int end = Math.min(offset + limit, bytes.length);

            // Walk back if the cut falls in the middle of a multi-byte UTF-8 sequence
            while (end < bytes.length && (bytes[end] & 0xC0) == 0x80) {
                end--;
            }

            if (!first) {
                sb.append(' ');
            }
            sb.append(new String(bytes, offset, end - offset, java.nio.charset.StandardCharsets.UTF_8));
            sb.append(CRLF);

            offset = end;
            first = false;
        }
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
