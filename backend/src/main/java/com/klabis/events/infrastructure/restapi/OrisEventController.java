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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@OrisIntegrationComponent
@RestController
@RequestMapping(value = "/api/events", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Events", description = "ORIS event import API")
@PrimaryAdapter
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.EVENTS_SCOPE})
class OrisEventController {

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

    @PostMapping(value = "/import", consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Import event from ORIS",
            description = "Creates a new event in DRAFT status by importing data from ORIS."
    )
    @ApiResponse(responseCode = "201", description = "Event imported successfully")
    public ResponseEntity<Void> importEvent(
            @Parameter(description = "ORIS import command with orisId")
            @Valid @RequestBody Event.ImportCommand command) {

        Event created = orisEventImportPort.importEventFromOris(command.orisId());

        return ResponseEntity
                .created(linkTo(methodOn(EventController.class).getEvent(created.getId().value(), null)).toUri())
                .build();
    }

    @PostMapping("/{id}/sync-from-oris")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Sync event from ORIS",
            description = "Re-fetches event data from ORIS and overwrites all local fields. Only allowed for DRAFT and ACTIVE events with an orisId."
    )
    @ApiResponse(responseCode = "204", description = "Event synced from ORIS successfully")
    public ResponseEntity<Void> syncEventFromOris(
            @Parameter(description = "Event UUID") @PathVariable UUID id) {

        orisEventImportPort.syncEventFromOris(new EventId(id));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync-from-oris/all-upcoming")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Bulk sync all upcoming ORIS events",
            description = "Synchronises all DRAFT/ACTIVE events with eventDate >= today that have an ORIS ID. "
                        + "Processes each event sequentially; partial failures are collected and returned in the summary. "
                        + "Always returns 200 — check failureCount in the response body."
    )
    @ApiResponse(responseCode = "200", description = "Bulk sync completed; inspect failureCount for partial failures")
    public ResponseEntity<EntityModel<BulkSyncResult>> syncAllUpcomingFromOris() {
        BulkSyncResult result = orisBulkSyncPort.syncAllUpcoming();
        EntityModel<BulkSyncResult> model = EntityModel.of(result);
        return ResponseEntity.ok(model);
    }

    @PostMapping(value = "/import-batch", consumes = "application/json")
    @HasAuthority(Authority.EVENTS_MANAGE)
    @Operation(
            summary = "Batch import events from ORIS",
            description = "Imports multiple ORIS events in a single request. Each event is processed independently; "
                        + "a failure to import one event does not prevent the others from being imported. "
                        + "Always returns 200 — check failureCount in the response body for partial failures."
    )
    @ApiResponse(responseCode = "200", description = "Batch import completed; inspect failureCount for partial failures")
    public ResponseEntity<EntityModel<BulkImportResult>> importEventsBatch(
            @Parameter(description = "Batch import command with list of ORIS event IDs")
            @Valid @RequestBody ImportBatchRequest request) {

        BulkImportResult result = orisEventBulkImportPort.importEventsFromOris(request.orisIds());
        return ResponseEntity.ok(EntityModel.of(result));
    }
}

record ImportBatchRequest(
        @NotEmpty(message = "orisIds must not be empty")
        @Size(max = 50, message = "orisIds must not contain more than 50 entries")
        List<Integer> orisIds
) {}
