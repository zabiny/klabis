package com.klabis.calendar.infrastructure.ical;

import com.klabis.calendar.application.IcalFeedPort.EventScheduleEntry;
import com.klabis.events.EventId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventStatus;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ICalendarRenderer")
class ICalendarRendererTest {

    private static final String BASE_URL = "https://klabis.example.com";
    private static final Instant DTSTAMP = Instant.parse("2026-05-21T10:00:00Z");
    private static final String CRLF = "\r\n";
    private static final UUID FIXED_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID COORDINATOR_UUID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final MemberId COORDINATOR_ID = new MemberId(COORDINATOR_UUID);

    private ICalendarRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new ICalendarRenderer();
    }

    private Event buildEvent(UUID id, String name, LocalDate date, String location,
                              String organizer, String websiteUrl, MemberId coordinatorId,
                              EventStatus status) {
        LinkedHashSet<MemberId> coordinators = new LinkedHashSet<>();
        if (coordinatorId != null) coordinators.add(coordinatorId);
        return Event.reconstruct(
                new EventId(id),
                name,
                date,
                location,
                organizer,
                websiteUrl != null ? WebsiteUrl.of(websiteUrl) : null,
                coordinators,
                null,
                null,
                status,
                null,
                null,
                List.of(),
                null,
                null,
                List.of(),
                null
        );
    }

    @Nested
    @DisplayName("render()")
    class RenderTests {

        @Test
        @DisplayName("should produce valid empty VCALENDAR when no events")
        void shouldRenderEmptyCalendar() {
            String result = renderer.render(List.of(), BASE_URL, DTSTAMP);

            assertThat(result).isEqualTo(
                    "BEGIN:VCALENDAR" + CRLF +
                    "VERSION:2.0" + CRLF +
                    "PRODID:-//Klabis//Klabis Member Portal//CS" + CRLF +
                    "END:VCALENDAR" + CRLF
            );
        }

        @Test
        @DisplayName("should render VEVENT for a registered participant (not coordinator)")
        void shouldRenderParticipantEvent() {
            Event event = buildEvent(FIXED_UUID, "Jarní Závod", LocalDate.of(2026, 6, 15),
                    "Praha", "ORG", null, COORDINATOR_ID, EventStatus.ACTIVE);

            String result = renderer.render(
                    List.of(new EventScheduleEntry(event, false)),
                    BASE_URL, DTSTAMP
            );

            assertThat(result).contains("BEGIN:VEVENT" + CRLF);
            assertThat(result).contains("UID:" + FIXED_UUID + "@klabis" + CRLF);
            assertThat(result).contains("DTSTAMP:20260521T100000Z" + CRLF);
            assertThat(result).contains("DTSTART;VALUE=DATE:20260615" + CRLF);
            assertThat(result).contains("DTEND;VALUE=DATE:20260616" + CRLF);
            assertThat(result).contains("SUMMARY:Jarní Závod" + CRLF);
            assertThat(result).contains("LOCATION:Praha" + CRLF);
            assertThat(result).contains("URL:https://klabis.example.com/events/" + FIXED_UUID + CRLF);
            assertThat(result).contains("STATUS:CONFIRMED" + CRLF);
            assertThat(result).contains("END:VEVENT" + CRLF);
            assertThat(result).doesNotContain("Role: koordinátor");
        }

        @Test
        @DisplayName("should render VEVENT for a coordinator with 'Role: koordinátor' in DESCRIPTION")
        void shouldRenderCoordinatorEvent() {
            Event event = buildEvent(FIXED_UUID, "Letní Sprint", LocalDate.of(2026, 7, 10),
                    "Brno", "MKO", null, COORDINATOR_ID, EventStatus.ACTIVE);

            String result = renderer.render(
                    List.of(new EventScheduleEntry(event, true)),
                    BASE_URL, DTSTAMP
            );

            assertThat(result).contains("DESCRIPTION:");
            assertThat(result).contains("Role: koordinátor");
        }

        @Test
        @DisplayName("should render STATUS:CANCELLED for a cancelled event")
        void shouldRenderCancelledEvent() {
            Event event = buildEvent(FIXED_UUID, "Zrušená Akce", LocalDate.of(2026, 8, 1),
                    null, "ORG", null, null, EventStatus.CANCELLED);

            String result = renderer.render(
                    List.of(new EventScheduleEntry(event, false)),
                    BASE_URL, DTSTAMP
            );

            assertThat(result).contains("STATUS:CANCELLED" + CRLF);
            assertThat(result).doesNotContain("STATUS:CONFIRMED");
        }

        @Test
        @DisplayName("should omit LOCATION when location is null")
        void shouldOmitLocationWhenNull() {
            Event event = buildEvent(FIXED_UUID, "Bez Lokace", LocalDate.of(2026, 6, 1),
                    null, "ORG", null, null, EventStatus.ACTIVE);

            String result = renderer.render(
                    List.of(new EventScheduleEntry(event, false)),
                    BASE_URL, DTSTAMP
            );

            assertThat(result).doesNotContain("LOCATION:");
        }

        @Test
        @DisplayName("should include website URL in DESCRIPTION when event has websiteUrl")
        void shouldIncludeWebsiteUrlInDescription() {
            Event event = buildEvent(FIXED_UUID, "Web Akce", LocalDate.of(2026, 9, 1),
                    "Olomouc", "KOB", "https://event.example.com", null, EventStatus.ACTIVE);

            String result = renderer.render(
                    List.of(new EventScheduleEntry(event, false)),
                    BASE_URL, DTSTAMP
            );

            // Unfold RFC 5545 folded lines (CRLF + SPACE is a continuation) before asserting logical content
            String unfolded = result.replace("\r\n ", "");
            assertThat(unfolded).contains("Web akce: https://event.example.com");
        }

        @Test
        @DisplayName("should escape commas and semicolons in SUMMARY")
        void shouldEscapeSpecialCharactersInSummary() {
            Event event = buildEvent(FIXED_UUID, "Závod; Praha, 2026", LocalDate.of(2026, 6, 1),
                    null, "ORG", null, null, EventStatus.ACTIVE);

            String result = renderer.render(
                    List.of(new EventScheduleEntry(event, false)),
                    BASE_URL, DTSTAMP
            );

            assertThat(result).contains("SUMMARY:Závod\\; Praha\\, 2026" + CRLF);
        }

        @Test
        @DisplayName("should escape backslash in text fields")
        void shouldEscapeBackslashInTextFields() {
            Event event = buildEvent(FIXED_UUID, "Path\\To\\Event", LocalDate.of(2026, 6, 1),
                    null, "ORG", null, null, EventStatus.ACTIVE);

            String result = renderer.render(
                    List.of(new EventScheduleEntry(event, false)),
                    BASE_URL, DTSTAMP
            );

            assertThat(result).contains("SUMMARY:Path\\\\To\\\\Event" + CRLF);
        }

        @Test
        @DisplayName("should fold DESCRIPTION lines exceeding 75 octets per RFC 5545 §3.1")
        void shouldFoldLongDescriptionLines() {
            String longOrganizer = "A".repeat(80);
            Event event = buildEvent(FIXED_UUID, "Akce", LocalDate.of(2026, 6, 15),
                    null, longOrganizer, null, COORDINATOR_ID, EventStatus.ACTIVE);

            String result = renderer.render(
                    List.of(new EventScheduleEntry(event, false)),
                    BASE_URL, DTSTAMP
            );

            // Each physical line (before CRLF) must be at most 75 octets
            String[] lines = result.split("\r\n", -1);
            for (String line : lines) {
                int octetLength = line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                org.assertj.core.api.Assertions.assertThat(octetLength)
                        .as("line exceeds 75 octets: %s", line)
                        .isLessThanOrEqualTo(75);
            }

            // Folded continuation lines start with a single space
            boolean hasfolding = false;
            for (String line : lines) {
                if (line.startsWith(" ")) {
                    hasfolding = true;
                    break;
                }
            }
            org.assertj.core.api.Assertions.assertThat(hasfolding)
                    .as("expected at least one folded continuation line starting with SPACE")
                    .isTrue();
        }

        @Test
        @DisplayName("should render two events correctly when member has both participant and coordinator roles")
        void shouldRenderBothRoles() {
            UUID uuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID uuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

            Event participantEvent = buildEvent(uuid1, "Participant Event", LocalDate.of(2026, 6, 10),
                    null, "ORG1", null, COORDINATOR_ID, EventStatus.ACTIVE);
            Event coordinatorEvent = buildEvent(uuid2, "Coordinator Event", LocalDate.of(2026, 7, 5),
                    null, "ORG2", null, COORDINATOR_ID, EventStatus.ACTIVE);

            String result = renderer.render(
                    List.of(
                            new EventScheduleEntry(participantEvent, false),
                            new EventScheduleEntry(coordinatorEvent, true)
                    ),
                    BASE_URL, DTSTAMP
            );

            assertThat(result).contains("UID:" + uuid1 + "@klabis");
            assertThat(result).contains("UID:" + uuid2 + "@klabis");
            assertThat(result).contains("SUMMARY:Participant Event");
            assertThat(result).contains("SUMMARY:Coordinator Event");

            // Only the coordinator event should have the role line
            long coordinatorLineCount = result.lines()
                    .filter(line -> line.contains("Role: koordinátor"))
                    .count();
            assertThat(coordinatorLineCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("escapeText()")
    class EscapeTextTests {

        @Test
        @DisplayName("should return empty string for null input")
        void shouldReturnEmptyForNull() {
            assertThat(ICalendarRenderer.escapeText(null)).isEmpty();
        }

        @Test
        @DisplayName("should escape backslash")
        void shouldEscapeBackslash() {
            assertThat(ICalendarRenderer.escapeText("a\\b")).isEqualTo("a\\\\b");
        }

        @Test
        @DisplayName("should escape comma")
        void shouldEscapeComma() {
            assertThat(ICalendarRenderer.escapeText("a,b")).isEqualTo("a\\,b");
        }

        @Test
        @DisplayName("should escape semicolon")
        void shouldEscapeSemicolon() {
            assertThat(ICalendarRenderer.escapeText("a;b")).isEqualTo("a\\;b");
        }

        @Test
        @DisplayName("should escape newline as \\n")
        void shouldEscapeNewline() {
            assertThat(ICalendarRenderer.escapeText("a\nb")).isEqualTo("a\\nb");
        }

        @Test
        @DisplayName("should escape CRLF as \\n")
        void shouldEscapeCrlf() {
            assertThat(ICalendarRenderer.escapeText("a\r\nb")).isEqualTo("a\\nb");
        }

        @Test
        @DisplayName("should escape backslash before comma and semicolon to avoid double-escaping")
        void shouldEscapeBackslashFirst() {
            assertThat(ICalendarRenderer.escapeText("a\\,b")).isEqualTo("a\\\\\\,b");
        }
    }
}
