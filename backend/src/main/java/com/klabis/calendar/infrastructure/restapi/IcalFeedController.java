package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.application.IcalFeedPort;
import com.klabis.calendar.application.IcalFeedPort.EventScheduleEntry;
import com.klabis.calendar.infrastructure.ical.ICalendarRenderer;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@PrimaryAdapter
@RestController
@Tag(name = "Calendar Feed", description = "iCalendar subscribe feed for personal schedule")
class IcalFeedController {

    private static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private final IcalFeedPort icalFeedPort;
    private final ICalendarRenderer icalRenderer;
    private final String baseUrl;

    IcalFeedController(
            IcalFeedPort icalFeedPort,
            ICalendarRenderer icalRenderer,
            @Value("${klabis.ical.base-url:https://localhost:8443}") String baseUrl) {
        this.icalFeedPort = icalFeedPort;
        this.icalRenderer = icalRenderer;
        this.baseUrl = baseUrl;
    }

    @GetMapping(value = "/ical/my-schedule.ics", produces = "text/calendar")
    @Operation(
            summary = "Personal schedule iCalendar feed",
            description = """
                    Returns an iCalendar (RFC 5545) feed for the authenticated user's personal schedule.
                    Includes events where the user is an active participant OR acts as coordinator.
                    Authenticate via the ?token= query parameter (Personal Access Token).
                    """
    )
    @ApiResponse(responseCode = "200", description = "iCalendar feed returned")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    ResponseEntity<String> getMySchedule(
            @Parameter(description = "Personal Access Token for calendar authentication", required = true)
            @RequestParam String token) {

        MemberId memberId = resolveAuthenticatedMemberId();
        List<EventScheduleEntry> entries = icalFeedPort.getMySchedule(memberId, LocalDate.now());
        String body = icalRenderer.render(entries, baseUrl, Instant.now());

        return ResponseEntity.ok()
                .contentType(TEXT_CALENDAR)
                .header(HttpHeaders.CACHE_CONTROL,
                        CacheControl.maxAge(600, TimeUnit.SECONDS).cachePublic().noTransform().getHeaderValue())
                .body(body);
    }

    private MemberId resolveAuthenticatedMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserId userId) {
            return MemberId.fromUserId(userId);
        }
        throw new IllegalStateException("Expected authenticated iCal token with UserId principal, got: "
                + (authentication != null ? authentication.getClass().getSimpleName() : "null"));
    }
}
