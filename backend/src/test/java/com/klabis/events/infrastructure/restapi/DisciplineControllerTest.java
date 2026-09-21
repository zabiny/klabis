package com.klabis.events.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.events.DisciplineId;
import com.klabis.events.application.DisciplineManagementPort;
import com.klabis.events.domain.Discipline;
import com.klabis.events.domain.DisciplineNotArchivedException;
import com.klabis.events.domain.DisciplineNotEditableException;
import com.klabis.events.domain.DisciplineNotFoundException;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncTarget;
import com.klabis.sync.domain.SyncedEntityReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("DisciplineController API tests")
@WebMvcTest(controllers = {DisciplineController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class DisciplineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DisciplineManagementPort disciplineManagementService;

    @MockitoBean
    private SynchronizationPort synchronizationPort;

    @BeforeEach
    void stubSynchronizationPortAbsentByDefault() {
        when(synchronizationPort.findByTarget(any())).thenReturn(Optional.empty());
        when(synchronizationPort.findActiveByTargets(any(), any())).thenReturn(List.of());
    }

    private static SyncTarget targetFor(DisciplineId id) {
        return new SyncTarget(SyncEntityType.DISCIPLINE, id.value().toString());
    }

    @Nested
    @DisplayName("GET /api/disciplines")
    class ListDisciplinesTests {

        @Test
        @DisplayName("should return 200 with a paginated list for user with EVENTS:READ")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldReturnPaginatedList() throws Exception {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineManagementService.list(any())).thenReturn(
                    new PageImpl<>(List.of(discipline), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/disciplines").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.disciplineDtoList[0].code").value("OB"))
                    .andExpect(jsonPath("$._embedded.disciplineDtoList[0].name").value("Orientační běh"))
                    .andExpect(jsonPath("$.page.size").value(10))
                    .andExpect(jsonPath("$.page.totalElements").value(1))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("should include first/last/next paging links when more than one page exists")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldReturnPagingLinksForMultiplePages() throws Exception {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineManagementService.list(any())).thenReturn(
                    new PageImpl<>(List.of(discipline), PageRequest.of(0, 1), 3));

            mockMvc.perform(get("/api/disciplines").param("page", "0").param("size", "1")
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.first.href").exists())
                    .andExpect(jsonPath("$._links.last.href").exists())
                    .andExpect(jsonPath("$._links.next.href").exists())
                    .andExpect(jsonPath("$._links.prev").doesNotExist());
        }

        @Test
        @DisplayName("should expose createDiscipline template on collection self link for user with EVENTS:MANAGE")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldExposeCreateTemplate() throws Exception {
            when(disciplineManagementService.list(any())).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/disciplines").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.createDiscipline.method").value("POST"));
        }

        @Test
        @DisplayName("should not expose createDiscipline template for user without EVENTS:MANAGE")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldNotExposeCreateTemplateWithoutManageAuthority() throws Exception {
            when(disciplineManagementService.list(any())).thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/disciplines").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.createDiscipline").doesNotExist());
        }

        @Test
        @DisplayName("should include sync link for a discipline paired to ORIS")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldIncludeSyncLinkForPairedDiscipline() throws Exception {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineManagementService.list(any())).thenReturn(
                    new PageImpl<>(List.of(discipline), PageRequest.of(0, 10), 1));
            when(synchronizationPort.findActiveByTargets(SyncEntityType.DISCIPLINE, List.of(discipline.getId().value().toString())))
                    .thenReturn(List.of(new SyncedEntityReference(
                            targetFor(discipline.getId()), new ExternalReference(ExternalSystem.ORIS, "100"))));

            mockMvc.perform(get("/api/disciplines").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.disciplineDtoList[0]._links.sync.href").exists());
        }

        @Test
        @DisplayName("should omit sync link for a manually created discipline")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldOmitSyncLinkForUnpairedDiscipline() throws Exception {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineManagementService.list(any())).thenReturn(
                    new PageImpl<>(List.of(discipline), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/disciplines").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.disciplineDtoList[0]._links.sync").doesNotExist());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/disciplines").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 when user has no event authorities at all")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(get("/api/disciplines").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/disciplines/{id}")
    class GetDisciplineTests {

        @Test
        @DisplayName("active, manually-created discipline: has updateDiscipline and archiveDiscipline, no restoreDiscipline")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldExposeUpdateAndArchiveForManuallyCreatedActiveDiscipline() throws Exception {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineManagementService.get(discipline.getId())).thenReturn(discipline);
            when(synchronizationPort.findByTarget(targetFor(discipline.getId()))).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/disciplines/{id}", discipline.getId().value())
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("OB"))
                    .andExpect(jsonPath("$.name").value("Orientační běh"))
                    .andExpect(jsonPath("$.archived").value(false))
                    .andExpect(jsonPath("$._templates.updateDiscipline.method").value("PUT"))
                    .andExpect(jsonPath("$._templates.archiveDiscipline.method").value("DELETE"))
                    .andExpect(jsonPath("$._templates.restoreDiscipline").doesNotExist())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").exists())
                    .andExpect(jsonPath("$._links.sync").doesNotExist());
        }

        @Test
        @DisplayName("archived, manually-created (unpaired) discipline: has restoreDiscipline and updateDiscipline, no archiveDiscipline")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldExposeRestoreAndUpdateForArchivedUnpairedDiscipline() throws Exception {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            discipline.archive();
            when(disciplineManagementService.get(discipline.getId())).thenReturn(discipline);
            when(synchronizationPort.findByTarget(targetFor(discipline.getId()))).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/disciplines/{id}", discipline.getId().value())
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.archived").value(true))
                    .andExpect(jsonPath("$._templates.restoreDiscipline.method").value("POST"))
                    .andExpect(jsonPath("$._templates.updateDiscipline.method").value("PUT"))
                    .andExpect(jsonPath("$._templates.archiveDiscipline").doesNotExist())
                    .andExpect(jsonPath("$._links.sync").doesNotExist());
        }

        @Test
        @DisplayName("ORIS-paired discipline: no updateDiscipline template, archiveDiscipline still present while active")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldHideUpdateTemplateForOrisPairedDiscipline() throws Exception {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            SyncRecord record = SyncRecord.enroll(SyncRecordId.newId(), targetFor(discipline.getId()),
                    new ExternalReference(ExternalSystem.ORIS, "100"));
            when(disciplineManagementService.get(discipline.getId())).thenReturn(discipline);
            when(synchronizationPort.findByTarget(targetFor(discipline.getId()))).thenReturn(Optional.of(record));

            mockMvc.perform(get("/api/disciplines/{id}", discipline.getId().value())
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateDiscipline").doesNotExist())
                    .andExpect(jsonPath("$._templates.archiveDiscipline.method").value("DELETE"))
                    .andExpect(jsonPath("$._templates.restoreDiscipline").doesNotExist())
                    .andExpect(jsonPath("$._links.sync.href").exists());
        }

        @Test
        @DisplayName("should not expose any management template for user without EVENTS:MANAGE")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldNotExposeManagementTemplatesWithoutManageAuthority() throws Exception {
            Discipline discipline = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineManagementService.get(discipline.getId())).thenReturn(discipline);
            when(synchronizationPort.findByTarget(targetFor(discipline.getId()))).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/disciplines/{id}", discipline.getId().value())
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateDiscipline").doesNotExist())
                    .andExpect(jsonPath("$._templates.archiveDiscipline").doesNotExist());
        }

        @Test
        @DisplayName("should return 404 when discipline not found")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(disciplineManagementService.get(new DisciplineId(id)))
                    .thenThrow(new DisciplineNotFoundException(new DisciplineId(id)));

            mockMvc.perform(get("/api/disciplines/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when user has no event authorities at all")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(get("/api/disciplines/{id}", UUID.randomUUID()).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/disciplines")
    class CreateDisciplineTests {

        @Test
        @DisplayName("should return 201 with Location header")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldCreateDiscipline() throws Exception {
            Discipline created = Discipline.create(new Discipline.CreateDiscipline("OB", "Orientační běh"));
            when(disciplineManagementService.create(any())).thenReturn(created);

            mockMvc.perform(post("/api/disciplines")
                            .contentType("application/json")
                            .content("""
                                    {"code": "OB", "name": "Orientační běh"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/disciplines/")));

            verify(disciplineManagementService).create(
                    new Discipline.CreateDiscipline("OB", "Orientační běh"));
        }

        @Test
        @DisplayName("should return 400 when code is missing")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn400WhenCodeMissing() throws Exception {
            mockMvc.perform(post("/api/disciplines")
                            .contentType("application/json")
                            .content("""
                                    {"name": "Orientační běh"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when missing authority")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(post("/api/disciplines")
                            .contentType("application/json")
                            .content("""
                                    {"code": "OB", "name": "Orientační běh"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/disciplines/{id}")
    class UpdateDisciplineTests {

        @Test
        @DisplayName("should return 204 on successful update")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldUpdateDiscipline() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(put("/api/disciplines/{id}", id)
                            .contentType("application/json")
                            .content("""
                                    {"code": "OB2", "name": "Orientační běh 2"}
                                    """))
                    .andExpect(status().isNoContent());

            verify(disciplineManagementService).update(new DisciplineId(id), "OB2", "Orientační běh 2");
        }

        @Test
        @DisplayName("should return 409 when discipline is ORIS-paired")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn409WhenNotEditable() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new DisciplineNotEditableException(new DisciplineId(id)))
                    .when(disciplineManagementService).update(any(), any(), any());

            mockMvc.perform(put("/api/disciplines/{id}", id)
                            .contentType("application/json")
                            .content("""
                                    {"code": "OB2", "name": "Orientační běh 2"}
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 404 when discipline not found")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new DisciplineNotFoundException(new DisciplineId(id)))
                    .when(disciplineManagementService).update(any(), any(), any());

            mockMvc.perform(put("/api/disciplines/{id}", id)
                            .contentType("application/json")
                            .content("""
                                    {"code": "OB2", "name": "Orientační běh 2"}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when missing authority")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(put("/api/disciplines/{id}", UUID.randomUUID())
                            .contentType("application/json")
                            .content("""
                                    {"code": "OB2", "name": "Orientační běh 2"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/disciplines/{id}")
    class ArchiveDisciplineTests {

        @Test
        @DisplayName("should return 204 on successful archive")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldArchiveDiscipline() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/disciplines/{id}", id))
                    .andExpect(status().isNoContent());

            verify(disciplineManagementService).archive(new DisciplineId(id));
        }

        @Test
        @DisplayName("should return 404 when discipline not found")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new DisciplineNotFoundException(new DisciplineId(id)))
                    .when(disciplineManagementService).archive(any());

            mockMvc.perform(delete("/api/disciplines/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when missing authority")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(delete("/api/disciplines/{id}", UUID.randomUUID()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/disciplines/{id}/restore")
    class RestoreDisciplineTests {

        @Test
        @DisplayName("should return 204 on successful restore")
        @WithKlabisMockUser(userId = "3fa85f64-5717-4562-b3fc-2c963f66afa6", authorities = {Authority.EVENTS_MANAGE})
        void shouldRestoreDiscipline() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(post("/api/disciplines/{id}/restore", id))
                    .andExpect(status().isNoContent());

            verify(disciplineManagementService).restore(
                    new DisciplineId(id), "3fa85f64-5717-4562-b3fc-2c963f66afa6");
        }

        @Test
        @DisplayName("should return 409 when discipline is not archived")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn409WhenNotArchived() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new DisciplineNotArchivedException(new DisciplineId(id)))
                    .when(disciplineManagementService).restore(any(), any());

            mockMvc.perform(post("/api/disciplines/{id}/restore", id))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 404 when discipline not found")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new DisciplineNotFoundException(new DisciplineId(id)))
                    .when(disciplineManagementService).restore(any(), any());

            mockMvc.perform(post("/api/disciplines/{id}/restore", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when missing authority")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(post("/api/disciplines/{id}/restore", UUID.randomUUID()))
                    .andExpect(status().isForbidden());
        }
    }
}
