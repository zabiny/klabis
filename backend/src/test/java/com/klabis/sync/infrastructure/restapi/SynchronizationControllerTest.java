package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.*;
import com.klabis.sync.domain.*;
import com.klabis.sync.fixtures.TestSyncProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for {@link SynchronizationController} (tasks.md 6.6): success and
 * refusal paths for each operation, per-state affordances, {@code SYNC:MANAGE} on every
 * operation, and the 404 cases (unenrolled entity, unknown entity type).
 */
@DisplayName("SynchronizationController")
@WebMvcTest(controllers = {SynchronizationController.class, SyncExceptionHandler.class})
@Import(HalFormsSupport.class)
@WithPostprocessors
class SynchronizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SynchronizationPort synchronizationPort;

    @MockitoBean
    private SyncProjectionFieldReader fieldReader;

    private static final SyncTarget TARGET = new SyncTarget(SyncEntityType.EVENT, "event-1");
    private static final ExternalReference EXTERNAL_REF = new ExternalReference(ExternalSystem.ORIS, "8123");

    private SyncRecord inSyncRecord() {
        SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), TARGET, EXTERNAL_REF);
        SyncSnapshot snapshot = SyncSnapshot.reconstruct(new TestSyncProjection("Sprint", "Brno"), SyncHash.of("h1"));
        record.recordSuccess(SyncDirection.INWARD, snapshot, snapshot);
        return record;
    }

    private SyncRecord conflictedRecord() {
        SyncRecord record = inSyncRecord();
        SyncSnapshot local = SyncSnapshot.reconstruct(new TestSyncProjection("Local", "Brno"), SyncHash.of("hl"));
        SyncSnapshot external = SyncSnapshot.reconstruct(new TestSyncProjection("External", "Brno"), SyncHash.of("he"));
        record.recordConflict(local, external, null);
        return record;
    }

    private SyncRecord acknowledgedConflictedRecord() {
        SyncRecord record = conflictedRecord();
        record.acknowledgeConflict(new ConflictAcknowledgement(
                record.getLocal().hash(), record.getExternal().hash(), Instant.now(), "admin"));
        return record;
    }

    private SyncRecord failedRecord() {
        SyncRecord record = inSyncRecord();
        record.recordTerminalFailure(5, "boom");
        return record;
    }

    @Nested
    @DisplayName("GET /api/{entityType}/{id}/sync")
    class GetSyncState {

        @Test
        @DisplayName("returns 200 with state and the synchronizeNow affordance while IN_SYNC")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returnsStateWithSynchronizeNowAffordance() throws Exception {
            when(fieldReader.fields(any())).thenReturn(java.util.Map.of("name", "Sprint"));
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(inSyncRecord()));
            when(synchronizationPort.failedAttemptsSinceLastSuccess(any())).thenReturn(0);

            mockMvc.perform(get("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("IN_SYNC"))
                    .andExpect(jsonPath("$._templates.synchronizeNow").exists())
                    .andExpect(jsonPath("$._templates.resetSyncRecord").doesNotExist());
        }

        @Test
        @DisplayName("returns the acknowledgeSyncConflict affordance while CONFLICT and not yet acknowledged")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returnsAcknowledgeAffordanceWhenConflictedAndNotAcknowledged() throws Exception {
            when(fieldReader.fields(any())).thenReturn(java.util.Map.of("name", "Local"));
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(conflictedRecord()));
            when(synchronizationPort.failedAttemptsSinceLastSuccess(any())).thenReturn(0);

            mockMvc.perform(get("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFLICT"))
                    .andExpect(jsonPath("$._templates.acknowledgeSyncConflict").exists())
                    .andExpect(jsonPath("$._templates.resolveSyncConflict").doesNotExist())
                    .andExpect(jsonPath("$._templates.synchronizeNow").doesNotExist());
        }

        @Test
        @DisplayName("returns the resolveSyncConflict affordance once acknowledged")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returnsResolveAffordanceWhenAcknowledged() throws Exception {
            when(fieldReader.fields(any())).thenReturn(java.util.Map.of("name", "Local"));
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(acknowledgedConflictedRecord()));
            when(synchronizationPort.failedAttemptsSinceLastSuccess(any())).thenReturn(0);

            mockMvc.perform(get("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.resolveSyncConflict").exists())
                    .andExpect(jsonPath("$._templates.acknowledgeSyncConflict").doesNotExist());
        }

        @Test
        @DisplayName("returns the resetSyncRecord affordance while FAILED")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returnsResetAffordanceWhenFailed() throws Exception {
            when(fieldReader.fields(any())).thenReturn(java.util.Map.of("name", "Sprint"));
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(failedRecord()));
            when(synchronizationPort.failedAttemptsSinceLastSuccess(any())).thenReturn(5);

            mockMvc.perform(get("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FAILED"))
                    .andExpect(jsonPath("$.failedAttemptsSinceLastSuccess").value(5))
                    .andExpect(jsonPath("$._templates.resetSyncRecord").exists())
                    .andExpect(jsonPath("$._templates.synchronizeNow").doesNotExist());
        }

        @Test
        @DisplayName("returns 404 when the entity is not enrolled")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns404WhenNotEnrolled() throws Exception {
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 404 for an unknown entity type")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns404ForUnknownEntityType() throws Exception {
            mockMvc.perform(get("/api/nonsense/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("requires SYNC:MANAGE")
        @WithKlabisMockUser(authorities = {})
        void requiresAuthority() throws Exception {
            mockMvc.perform(get("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/{entityType}/{id}/sync")
    class SynchronizeNow {

        @Test
        @DisplayName("returns 200 with the resulting state")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returnsResultingState() throws Exception {
            when(fieldReader.fields(any())).thenReturn(java.util.Map.of());
            SyncRecord enrolled = inSyncRecord();
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(enrolled));
            when(synchronizationPort.synchronizeNow(eq(enrolled.getId()), any())).thenReturn(enrolled);
            when(synchronizationPort.failedAttemptsSinceLastSuccess(any())).thenReturn(0);

            mockMvc.perform(post("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk());

            verify(synchronizationPort).synchronizeNow(eq(enrolled.getId()), any());
        }

        @Test
        @DisplayName("returns 409 when the record needs a decision")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns409WhenNeedsResolution() throws Exception {
            SyncRecord conflicted = conflictedRecord();
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(conflicted));
            when(synchronizationPort.synchronizeNow(eq(conflicted.getId()), any()))
                    .thenThrow(new SyncRecordNeedsResolutionException(conflicted.getId(), SyncStatus.CONFLICT));

            mockMvc.perform(post("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 404 when the entity is not enrolled")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns404WhenNotEnrolled() throws Exception {
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("requires SYNC:MANAGE")
        @WithKlabisMockUser(authorities = {})
        void requiresAuthority() throws Exception {
            mockMvc.perform(post("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 404 for an unknown entity type")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns404ForUnknownEntityType() throws Exception {
            mockMvc.perform(post("/api/nonsense/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/{entityType}/{id}/sync/acknowledgement")
    class AcknowledgeSyncConflict {

        @Test
        @DisplayName("returns 200 when acknowledged")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns200WhenAcknowledged() throws Exception {
            when(fieldReader.fields(any())).thenReturn(java.util.Map.of());
            SyncRecord conflicted = conflictedRecord();
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(conflicted));
            when(synchronizationPort.acknowledgeConflict(eq(conflicted.getId()), any())).thenReturn(conflicted);
            when(synchronizationPort.failedAttemptsSinceLastSuccess(any())).thenReturn(0);

            mockMvc.perform(post("/api/events/{id}/sync/acknowledgement", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 409 when the record is not in conflict")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns409WhenNotInConflict() throws Exception {
            SyncRecord record = inSyncRecord();
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(record));
            when(synchronizationPort.acknowledgeConflict(eq(record.getId()), any()))
                    .thenThrow(new SyncRecordNotInConflictException(record.getId()));

            mockMvc.perform(post("/api/events/{id}/sync/acknowledgement", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("requires SYNC:MANAGE")
        @WithKlabisMockUser(authorities = {})
        void requiresAuthority() throws Exception {
            mockMvc.perform(post("/api/events/{id}/sync/acknowledgement", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 404 when the entity is not enrolled")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns404WhenNotEnrolled() throws Exception {
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/events/{id}/sync/acknowledgement", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 404 for an unknown entity type")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns404ForUnknownEntityType() throws Exception {
            mockMvc.perform(post("/api/nonsense/{id}/sync/acknowledgement", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/{entityType}/{id}/sync/resolution")
    class ResolveSyncConflict {

        @Test
        @DisplayName("returns 200 when resolved")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns200WhenResolved() throws Exception {
            when(fieldReader.fields(any())).thenReturn(java.util.Map.of());
            SyncRecord record = acknowledgedConflictedRecord();
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(record));
            when(synchronizationPort.resolveConflict(eq(record.getId()), eq(SyncResolution.INWARD), any())).thenReturn(record);
            when(synchronizationPort.failedAttemptsSinceLastSuccess(any())).thenReturn(0);

            mockMvc.perform(post("/api/events/{id}/sync/resolution", "event-1")
                            .contentType("application/json")
                            .content("{\"resolution\":\"INWARD\"}")
                            .accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 409 when not acknowledged")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns409WhenNotAcknowledged() throws Exception {
            SyncRecord record = conflictedRecord();
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(record));
            when(synchronizationPort.resolveConflict(eq(record.getId()), any(), any()))
                    .thenThrow(new ConflictNotAcknowledgedException(record.getId()));

            mockMvc.perform(post("/api/events/{id}/sync/resolution", "event-1")
                            .contentType("application/json")
                            .content("{\"resolution\":\"INWARD\"}")
                            .accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 for an unsupported resolution direction")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns409WhenUnsupported() throws Exception {
            SyncRecord record = acknowledgedConflictedRecord();
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(record));
            when(synchronizationPort.resolveConflict(eq(record.getId()), eq(SyncResolution.OUTWARD), any()))
                    .thenThrow(new UnsupportedResolutionException(record.getId(), SyncResolution.OUTWARD));

            mockMvc.perform(post("/api/events/{id}/sync/resolution", "event-1")
                            .contentType("application/json")
                            .content("{\"resolution\":\"OUTWARD\"}")
                            .accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("requires SYNC:MANAGE")
        @WithKlabisMockUser(authorities = {})
        void requiresAuthority() throws Exception {
            mockMvc.perform(post("/api/events/{id}/sync/resolution", "event-1")
                            .contentType("application/json")
                            .content("{\"resolution\":\"INWARD\"}")
                            .accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 404 when the entity is not enrolled")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns404WhenNotEnrolled() throws Exception {
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/events/{id}/sync/resolution", "event-1")
                            .contentType("application/json")
                            .content("{\"resolution\":\"INWARD\"}")
                            .accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 404 for an unknown entity type")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns404ForUnknownEntityType() throws Exception {
            mockMvc.perform(post("/api/nonsense/{id}/sync/resolution", "event-1")
                            .contentType("application/json")
                            .content("{\"resolution\":\"INWARD\"}")
                            .accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/{entityType}/{id}/sync/reset")
    class ResetSyncRecord {

        @Test
        @DisplayName("returns 200 when reset")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns200WhenReset() throws Exception {
            when(fieldReader.fields(any())).thenReturn(java.util.Map.of());
            SyncRecord failed = failedRecord();
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(failed));
            when(synchronizationPort.reset(eq(failed.getId()), any())).thenReturn(inSyncRecord());
            when(synchronizationPort.failedAttemptsSinceLastSuccess(any())).thenReturn(0);

            mockMvc.perform(post("/api/events/{id}/sync/reset", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 409 when the record is not failed")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns409WhenNotFailed() throws Exception {
            SyncRecord record = inSyncRecord();
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.of(record));
            when(synchronizationPort.reset(eq(record.getId()), any()))
                    .thenThrow(new SyncRecordNotFailedException(record.getId()));

            mockMvc.perform(post("/api/events/{id}/sync/reset", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("requires SYNC:MANAGE")
        @WithKlabisMockUser(authorities = {})
        void requiresAuthority() throws Exception {
            mockMvc.perform(post("/api/events/{id}/sync/reset", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 404 when the entity is not enrolled")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns404WhenNotEnrolled() throws Exception {
            when(synchronizationPort.findByTarget(TARGET)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/events/{id}/sync/reset", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 404 for an unknown entity type")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void returns404ForUnknownEntityType() throws Exception {
            mockMvc.perform(post("/api/nonsense/{id}/sync/reset", "event-1").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("AmbiguousSyncTargetException handling")
    class AmbiguousSyncTarget {

        @Test
        @DisplayName("propagates as 500 Internal Server Error (deliberate, see AmbiguousSyncTargetException javadoc)")
        @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
        void propagatesAsInternalServerError() {
            when(synchronizationPort.findByTarget(TARGET))
                    .thenThrow(new AmbiguousSyncTargetException(SyncEntityType.EVENT, 2));

            // No @ExceptionHandler catches this exception (deliberate — see javadoc), so
            // it is not translated into an HTTP response within the dispatch: MockMvc
            // re-throws it wrapped in a ServletException, exactly as a real container
            // would surface an unhandled exception before falling back to its own
            // default 500 error page.
            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                            mockMvc.perform(get("/api/events/{id}/sync", "event-1").accept(MediaTypes.HAL_FORMS_JSON)))
                    .hasRootCauseInstanceOf(AmbiguousSyncTargetException.class);
        }
    }
}
