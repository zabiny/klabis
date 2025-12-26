package club.klabis.events.infrastructure.restapi;

import club.klabis.events.domain.Event;
import club.klabis.events.oris.OrisEventSynchronizationUseCase;
import club.klabis.events.oris.dto.OrisEventListFilter;
import club.klabis.shared.application.OrisIntegrationComponent;
import club.klabis.shared.config.restapi.ApiController;
import club.klabis.shared.config.security.ApplicationGrant;
import club.klabis.shared.config.security.HasGrant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "ORIS")
@ApiController(openApiTagName = "ORIS", securityScopes = {"oris"})
@OrisIntegrationComponent
class OrisEventsController {

    private final OrisEventSynchronizationUseCase orisEventSynchronizationUseCase;

    public OrisEventsController(OrisEventSynchronizationUseCase orisEventSynchronizationUseCase) {
        this.orisEventSynchronizationUseCase = orisEventSynchronizationUseCase;
    }

    @Operation(
            operationId = "synchronizeAllEventsWithOris",
            summary = "Triggers events synchronization with ORIS",
            description = "#### Required authorization requires `system:admin` grant ",
            tags = {"ORIS"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully triggered events synchronization"),
                    @ApiResponse(responseCode = "401", description = "Missing required user authentication or authentication failed"),
                    @ApiResponse(responseCode = "403", description = "User is not allowed to perform requested operation")
            }
    )
    @PostMapping("/events/oris-synchronization")
    @HasGrant(ApplicationGrant.SYSTEM_ADMIN)
    public ResponseEntity<Void> synchronizeAllEventsWithOris() {
        orisEventSynchronizationUseCase.loadOrisEvents(OrisEventListFilter.createDefault()
                .withOfficialOnly(false)
                .withDateFrom(LocalDate.now().minusMonths(3))
                .withDateTo(LocalDate.now().plusMonths(6)));
        return ResponseEntity.ok(null);
    }

    @Operation(
            operationId = "SynchronizeEventWithOris",
            summary = "Triggers events synchronization with ORIS",
            description = "#### Required authorization requires `system:admin` grant ",
            tags = {"ORIS"},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Successfully triggered events synchronization"),
                    @ApiResponse(responseCode = "401", description = "Missing required user authentication or authentication failed"),
                    @ApiResponse(responseCode = "403", description = "User is not allowed to perform requested operation")
            }
    )
    @PostMapping("/events/{eventId}/synchronizeWithOris")
    @HasGrant(ApplicationGrant.SYSTEM_ADMIN)
    public ResponseEntity<Void> synchronizeEventWithOris(@PathVariable("eventId") Event.Id eventId) {
        orisEventSynchronizationUseCase.synchronizeEvents(List.of(eventId));
        return ResponseEntity.ok(null);
    }

}
