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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            description = "Returns events from ORIS available for import. Accepts multiple region parameters to combine results."
    )
    public ResponseEntity<List<OrisEventSummary>> listOrisEvents(
            @RequestParam(required = false) List<String> region) {

        List<String> regions = (region == null || region.isEmpty())
                ? List.of(OrisApiClient.REGION_JIHOMORAVSKA)
                : region;

        List<OrisEventSummary> events = regions.stream()
                .flatMap(rg -> {
                    OrisEventListFilter filter = OrisEventListFilter.EMPTY
                            .withRegion(rg)
                            .withDateFrom(LocalDate.now())
                            .withDateTo(LocalDate.now().plusYears(1));
                    return orisApiClient.getEventList(filter).payload()
                            .map(eventMap -> eventMap.values().stream())
                            .orElse(Stream.empty());
                })
                .map(e -> new OrisEventSummary(e.id(), e.name(), e.date(), e.location(),
                        e.organizer1() != null ? e.organizer1().abbreviation() : null))
                .collect(Collectors.toMap(OrisEventSummary::id, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparing(OrisEventSummary::date))
                .toList();

        return ResponseEntity.ok(events);
    }

    public record OrisEventSummary(int id, String name, LocalDate date, String location, String organizer) {
    }
}
