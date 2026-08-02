package com.klabis.oris;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisEventListFilter;
import com.dpolach.api.orisclient.OrisRegion;
import com.klabis.events.application.ImportedOrisEventsPort;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OrisIntegrationComponent
@RestController
@PrimaryAdapter
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class OrisController implements OrisImportApi {

    private final OrisApiClient orisApiClient;
    private final ImportedOrisEventsPort importedOrisEventsPort;

    OrisController(OrisApiClient orisApiClient, ImportedOrisEventsPort importedOrisEventsPort) {
        this.orisApiClient = orisApiClient;
        this.importedOrisEventsPort = importedOrisEventsPort;
    }

    @Override
    public ResponseEntity<List<OrisEventSummary>> listOrisEvents(List<String> region) {
        List<OrisRegion> regions = (region == null || region.isEmpty())
                ? List.of(OrisRegion.JIHOMORAVSKA)
                : region.stream().map(OrisRegion::valueOf).toList();

        List<OrisEventSummary> orisEvents = regions.stream()
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
                .toList();

        List<Integer> candidateIds = orisEvents.stream().map(OrisEventSummary::id).toList();
        Set<Integer> alreadyImported = importedOrisEventsPort.findImportedOrisIds(candidateIds);

        List<OrisEventSummary> events = orisEvents.stream()
                .filter(e -> !alreadyImported.contains(e.id()))
                .sorted(Comparator.comparing(OrisEventSummary::date))
                .toList();

        return ResponseEntity.ok(events);
    }
}
