package com.klabis.oris;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.oris.apiclient.OrisApiClient;
import com.klabis.oris.apiclient.OrisEventListFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@OrisIntegrationComponent
@RestController
@PrimaryAdapter
@RequestMapping(value = "/api/oris", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "ORIS", description = "ORIS orienteering system integration API")
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.EVENTS_SCOPE})
public class OrisController {

    private final OrisApiClient orisApiClient;

    OrisController(OrisApiClient orisApiClient) {
        this.orisApiClient = orisApiClient;
    }

    @GetMapping("/events")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "List upcoming ORIS events",
            description = "Returns events from ORIS available for import (today to +1 year)."
    )
    public ResponseEntity<List<OrisEventSummary>> listOrisEvents() {
        OrisEventListFilter filter = OrisEventListFilter.EMPTY
                .withDateFrom(LocalDate.now())
                .withDateTo(LocalDate.now().plusYears(1));

        var payload = orisApiClient.getEventList(filter).payload();
        List<OrisEventSummary> events = payload
                .map(eventMap -> eventMap.values().stream()
                        .map(e -> new OrisEventSummary(e.id(), e.name(), e.date()))
                        .toList())
                .orElse(List.of());

        return ResponseEntity.ok(events);
    }

    public record OrisEventSummary(int id, String name, LocalDate date) {
    }
}
