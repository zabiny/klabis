package com.klabis.events.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.EventId;
import com.klabis.events.application.OrisEventImportPort;
import com.klabis.events.domain.Event;
import com.klabis.oris.OrisIntegrationComponent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    OrisEventController(OrisEventImportPort orisEventImportPort) {
        this.orisEventImportPort = orisEventImportPort;
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
}
