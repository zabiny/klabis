package com.klabis.events.infrastructure.restapi;

import com.klabis.common.users.HasAuthority;
import com.klabis.events.EventId;
import com.klabis.events.application.BulkImportResult;
import com.klabis.events.application.BulkSyncResult;
import com.klabis.events.application.OrisBulkSyncPort;
import com.klabis.events.application.OrisEventBulkImportPort;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.oris.OrisIntegrationComponent;
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
@PrimaryAdapter
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

    @Override
    public ResponseEntity<Void> importEvent(
            ImportCommand command) {

        Event created = orisEventImportPort.importEventFromOris(command.orisId());

        return ResponseEntity
                .created(linkTo(methodOn(EventsApi.class).getEvent(created.getId().value(), null)).toUri())
                .build();
    }

    @Override
    public ResponseEntity<Void> syncEventFromOris(
            @PathVariable UUID id) {

        orisEventImportPort.syncEventFromOris(new EventId(id));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<BulkSyncResult> syncAllUpcomingFromOris() {
        BulkSyncResult result = orisBulkSyncPort.syncAllUpcoming();
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<BulkImportResult> importEventsBatch(
            ImportBatchRequest request) {

        BulkImportResult result = orisEventBulkImportPort.importEventsFromOris(request.orisIds());
        return ResponseEntity.ok(result);
    }
}
