package com.klabis.events.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.EventId;
import com.klabis.events.application.BulkImportResult;
import com.klabis.events.application.BulkSyncResult;
import com.klabis.events.application.OrisBulkSyncPort;
import com.klabis.events.application.OrisEventBulkImportPort;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.oris.OrisIntegrationComponent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@OrisIntegrationComponent
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "OrisEvents", description = "ORIS event import API")
@PrimaryAdapter
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.EVENTS_SCOPE})
class OrisEventController implements OrisEventsApi {

    private final OrisEventImportPort orisEventImportPort;
    private final OrisEventBulkImportPort orisEventBulkImportPort;
    private final OrisBulkSyncPort orisBulkSyncPort;

    OrisEventController(OrisEventImportPort orisEventImportPort,
                        OrisEventBulkImportPort orisEventBulkImportPort,
                        OrisBulkSyncPort orisBulkSyncPort) {
        this.orisEventImportPort = orisEventImportPort;
        this.orisEventBulkImportPort = orisEventBulkImportPort;
        this.orisBulkSyncPort = orisBulkSyncPort;
    }

    @Operation(
            summary = "Import event from ORIS",
            description = "Creates a new event in DRAFT status by importing data from ORIS."
    )
    @ApiResponse(responseCode = "201", description = "Event imported successfully")
    @Override
    public ResponseEntity<Void> importEvent(
            @Parameter(description = "ORIS import command with orisId")
            ImportCommand command) {

        Event created = orisEventImportPort.importEventFromOris(command.orisId());

        return ResponseEntity
                .created(linkTo(methodOn(EventsApi.class).getEvent(created.getId().value(), null)).toUri())
                .build();
    }

    @Operation(
            summary = "Sync event from ORIS",
            description = "Re-fetches event data from ORIS and overwrites all local fields. Only allowed for DRAFT and ACTIVE events with an orisId."
    )
    @ApiResponse(responseCode = "204", description = "Event synced from ORIS successfully")
    @Override
    public ResponseEntity<Void> syncEventFromOris(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        orisEventImportPort.syncEventFromOris(new EventId(id));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Bulk sync all upcoming ORIS events",
            description = "Synchronises all DRAFT/ACTIVE events with eventDate >= today that have an ORIS ID. "
                        + "Processes each event sequentially; partial failures are collected and returned in the summary. "
                        + "Always returns 200 — check failureCount in the response body."
    )
    @ApiResponse(responseCode = "200", description = "Bulk sync completed; inspect failureCount for partial failures")
    @Override
    public ResponseEntity<BulkSyncResult> syncAllUpcomingFromOris() {
        BulkSyncResult result = orisBulkSyncPort.syncAllUpcoming();
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Batch import events from ORIS",
            description = "Imports multiple ORIS events in a single request. Each event is processed independently; "
                        + "a failure to import one event does not prevent the others from being imported. "
                        + "Always returns 200 — check failureCount in the response body for partial failures."
    )
    @ApiResponse(responseCode = "200", description = "Batch import completed; inspect failureCount for partial failures")
    @Override
    public ResponseEntity<BulkImportResult> importEventsBatch(
            @Parameter(description = "Batch import command with list of ORIS event IDs")
            ImportBatchRequest request) {

        BulkImportResult result = orisEventBulkImportPort.importEventsFromOris(request.orisIds());
        return ResponseEntity.ok(result);
    }
}
