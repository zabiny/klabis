package com.klabis.events.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsInlineOption;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.events.DisciplineId;
import com.klabis.events.EventTypeId;
import com.klabis.events.application.EventTypeManagementPort;
import com.klabis.events.domain.EventType;
import com.klabis.events.domain.EventTypeInUseException;
import com.klabis.events.domain.EventTypeNotFoundException;
import com.klabis.events.domain.OrisDisciplineAlreadyMappedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("EventTypeController API tests")
@WebMvcTest(controllers = {EventTypeController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class EventTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventTypeManagementPort eventTypeManagementService;

    @BeforeEach
    void stubDisciplineOptions() {
        when(eventTypeManagementService.listDisciplineOptions()).thenReturn(List.<HalFormsInlineOption>of());
    }

    @Nested
    @DisplayName("GET /api/event-types")
    class ListEventTypesTests {

        @Test
        @DisplayName("should return 200 with list of event types for user with EVENTS:READ")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldReturnListOfEventTypes() throws Exception {
            EventType eventType = EventType.create(new EventType.CreateEventType("Trénink", "#ff0000", 1, null), 1);
            when(eventTypeManagementService.listAllSorted()).thenReturn(List.of(eventType));

            mockMvc.perform(get("/api/event-types").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventTypeDtoList[0].name").value("Trénink"))
                    .andExpect(jsonPath("$._embedded.eventTypeDtoList[0].color").value("#ff0000"))
                    .andExpect(jsonPath("$._embedded.eventTypeDtoList[0].sortOrder").value(1));
        }

        @Test
        @DisplayName("disciplineIds in event type DTO reflect the event type's local discipline ids")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldReturnDisciplineIdsInDto() throws Exception {
            DisciplineId disciplineId1 = DisciplineId.generate();
            DisciplineId disciplineId2 = DisciplineId.generate();
            EventType eventType = EventType.create(
                    new EventType.CreateEventType("Trénink", "#ff0000", 1, Set.of(disciplineId1, disciplineId2)), 1);
            when(eventTypeManagementService.listAllSorted()).thenReturn(List.of(eventType));

            mockMvc.perform(get("/api/event-types").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.eventTypeDtoList[0].disciplineIds").isArray())
                    .andExpect(jsonPath("$._embedded.eventTypeDtoList[0].disciplineIds", hasSize(2)))
                    .andExpect(jsonPath("$._embedded.eventTypeDtoList[0].disciplineIds", containsInAnyOrder(
                            disciplineId1.value().toString(), disciplineId2.value().toString())));
        }

        @Test
        @DisplayName("should expose createEventType template on collection self link for admin user with EVENTS:READ and EVENTS:MANAGE")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldExposeCreateTemplate() throws Exception {
            when(eventTypeManagementService.listAllSorted()).thenReturn(List.of());

            mockMvc.perform(get("/api/event-types").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.createEventType").exists())
                    .andExpect(jsonPath("$._templates.createEventType.method").value("POST"));
        }

        @Test
        @DisplayName("createEventType template should include disciplineIds property with inline value+prompt options from the local catalog")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeDisciplineIdsWithInlineOptionsInCreateTemplate() throws Exception {
            when(eventTypeManagementService.listAllSorted()).thenReturn(List.of());
            when(eventTypeManagementService.listDisciplineOptions()).thenReturn(List.of(
                    new HalFormsInlineOption("1", "Orientační běh"),
                    new HalFormsInlineOption("3", "Lyžařský OB"),
                    new HalFormsInlineOption("7", "Sprint")
            ));

            mockMvc.perform(get("/api/event-types").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.createEventType.properties[?(@.name=='disciplineIds')]").exists())
                    .andExpect(jsonPath("$._templates.createEventType.properties[?(@.name=='disciplineIds')].options.inline").isArray())
                    .andExpect(jsonPath("$._templates.createEventType.properties[?(@.name=='disciplineIds')].options.inline[0].value").value("1"))
                    .andExpect(jsonPath("$._templates.createEventType.properties[?(@.name=='disciplineIds')].options.inline[0].prompt").value("Orientační běh"))
                    .andExpect(jsonPath("$._templates.createEventType.properties[?(@.name=='disciplineIds')].options.inline[1].value").value("3"))
                    .andExpect(jsonPath("$._templates.createEventType.properties[?(@.name=='disciplineIds')].options.inline[1].prompt").value("Lyžařský OB"))
                    .andExpect(jsonPath("$._templates.createEventType.properties[?(@.name=='disciplineIds')].options.inline[2].value").value("7"))
                    .andExpect(jsonPath("$._templates.createEventType.properties[?(@.name=='disciplineIds')].options.inline[2].prompt").value("Sprint"))
                    .andExpect(jsonPath("$._templates.createEventType.properties[?(@.name=='disciplineIds')].options.promptField").value("prompt"))
                    .andExpect(jsonPath("$._templates.createEventType.properties[?(@.name=='disciplineIds')].options.valueField").value("value"));
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/event-types").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 when user has no event authorities at all")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(get("/api/event-types").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/event-types/{id}")
    class GetEventTypeTests {

        @Test
        @DisplayName("should return 200 with event type details for user with EVENTS:READ")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldReturnEventTypeDetails() throws Exception {
            UUID id = UUID.randomUUID();
            EventType eventType = EventType.create(new EventType.CreateEventType("Závod", "#00ff00", 2, null), 2);
            when(eventTypeManagementService.getEventType(any(EventTypeId.class))).thenReturn(eventType);

            mockMvc.perform(get("/api/event-types/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Závod"))
                    .andExpect(jsonPath("$.color").value("#00ff00"));
        }

        @Test
        @DisplayName("disciplineIds in event type detail DTO reflect the event type's local discipline ids")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldReturnDisciplineIdsInDetailDto() throws Exception {
            UUID id = UUID.randomUUID();
            DisciplineId disciplineId = DisciplineId.generate();
            EventType eventType = EventType.create(new EventType.CreateEventType("Závod", "#00ff00", 2, Set.of(disciplineId)), 2);
            when(eventTypeManagementService.getEventType(any(EventTypeId.class))).thenReturn(eventType);

            mockMvc.perform(get("/api/event-types/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.disciplineIds").isArray())
                    .andExpect(jsonPath("$.disciplineIds", hasSize(1)))
                    .andExpect(jsonPath("$.disciplineIds[0]").value(disciplineId.value().toString()));
        }

        @Test
        @DisplayName("should return 404 when event type not found")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ})
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(eventTypeManagementService.getEventType(any(EventTypeId.class)))
                    .thenThrow(new EventTypeNotFoundException(new EventTypeId(id)));

            mockMvc.perform(get("/api/event-types/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should expose updateEventType and deleteEventType templates for admin user with EVENTS:READ and EVENTS:MANAGE")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldExposeUpdateAndDeleteTemplates() throws Exception {
            UUID id = UUID.randomUUID();
            EventType eventType = EventType.create(new EventType.CreateEventType("Závod", null, 1, null), 1);
            when(eventTypeManagementService.getEventType(any(EventTypeId.class))).thenReturn(eventType);

            mockMvc.perform(get("/api/event-types/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateEventType.method").value("PUT"))
                    .andExpect(jsonPath("$._templates.deleteEventType.method").value("DELETE"));
        }

        @Test
        @DisplayName("updateEventType template should include disciplineIds property with inline value+prompt options from the local catalog")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_READ, Authority.EVENTS_MANAGE})
        void shouldIncludeDisciplineIdsWithInlineOptionsInUpdateTemplate() throws Exception {
            UUID id = UUID.randomUUID();
            EventType eventType = EventType.create(new EventType.CreateEventType("Závod", null, 1, null), 1);
            when(eventTypeManagementService.getEventType(any(EventTypeId.class))).thenReturn(eventType);
            when(eventTypeManagementService.listDisciplineOptions()).thenReturn(List.of(
                    new HalFormsInlineOption("1", "Orientační běh"),
                    new HalFormsInlineOption("3", "Lyžařský OB")
            ));

            mockMvc.perform(get("/api/event-types/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateEventType.properties[?(@.name=='disciplineIds')]").exists())
                    .andExpect(jsonPath("$._templates.updateEventType.properties[?(@.name=='disciplineIds')].options.inline").isArray())
                    .andExpect(jsonPath("$._templates.updateEventType.properties[?(@.name=='disciplineIds')].options.inline[0].value").value("1"))
                    .andExpect(jsonPath("$._templates.updateEventType.properties[?(@.name=='disciplineIds')].options.inline[0].prompt").value("Orientační běh"))
                    .andExpect(jsonPath("$._templates.updateEventType.properties[?(@.name=='disciplineIds')].options.inline[1].value").value("3"))
                    .andExpect(jsonPath("$._templates.updateEventType.properties[?(@.name=='disciplineIds')].options.inline[1].prompt").value("Lyžařský OB"))
                    .andExpect(jsonPath("$._templates.updateEventType.properties[?(@.name=='disciplineIds')].options.promptField").value("prompt"))
                    .andExpect(jsonPath("$._templates.updateEventType.properties[?(@.name=='disciplineIds')].options.valueField").value("value"));
        }

        @Test
        @DisplayName("should return 403 when user has no event authorities at all")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(get("/api/event-types/{id}", UUID.randomUUID()).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/event-types")
    class CreateEventTypeTests {

        @Test
        @DisplayName("should return 201 with Location header")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldCreateEventType() throws Exception {
            EventType created = EventType.create(new EventType.CreateEventType("Trénink", null, null, null), 1);
            when(eventTypeManagementService.createEventType(any())).thenReturn(created);

            mockMvc.perform(post("/api/event-types")
                            .contentType("application/json")
                            .content("""
                                    {"name": "Trénink"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/event-types/")));
        }

        @Test
        @DisplayName("disciplineIds from request body are resolved to local DisciplineIds and passed to the service")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldPassDisciplineIdsToServiceOnCreate() throws Exception {
            DisciplineId disciplineId = DisciplineId.generate();
            EventType created = EventType.create(new EventType.CreateEventType("Trénink", null, null, Set.of(disciplineId)), 1);
            when(eventTypeManagementService.createEventType(any())).thenReturn(created);

            mockMvc.perform(post("/api/event-types")
                            .contentType("application/json")
                            .content("""
                                    {"name": "Trénink", "disciplineIds": ["%s"]}
                                    """.formatted(disciplineId.value())))
                    .andExpect(status().isCreated());

            verify(eventTypeManagementService).createEventType(
                    argThat(cmd -> cmd.disciplineIds().equals(Set.of(disciplineId))));
        }

        @Test
        @DisplayName("should return 400 when name is missing")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn400WhenNameMissing() throws Exception {
            mockMvc.perform(post("/api/event-types")
                            .contentType("application/json")
                            .content("""
                                    {"color": "#ff0000"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 409 when ORIS discipline already mapped to another event type")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn409WhenOrisDisciplineAlreadyMapped() throws Exception {
            when(eventTypeManagementService.createEventType(any()))
                    .thenThrow(new OrisDisciplineAlreadyMappedException());

            mockMvc.perform(post("/api/event-types")
                            .contentType("application/json")
                            .content("""
                                    {"name": "Trénink"}
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 403 when missing authority")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(post("/api/event-types")
                            .contentType("application/json")
                            .content("""
                                    {"name": "Trénink"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/event-types/{id}")
    class UpdateEventTypeTests {

        @Test
        @DisplayName("should return 204 on successful update")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldUpdateEventType() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(put("/api/event-types/{id}", id)
                            .contentType("application/json")
                            .content("""
                                    {"name": "Updated Name"}
                                    """))
                    .andExpect(status().isNoContent());

            verify(eventTypeManagementService).updateEventType(eq(new EventTypeId(id)), any());
        }

        @Test
        @DisplayName("disciplineIds from request body are resolved to local DisciplineIds and passed to the service")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldPassDisciplineIdsToServiceOnUpdate() throws Exception {
            UUID id = UUID.randomUUID();
            DisciplineId disciplineId1 = DisciplineId.generate();
            DisciplineId disciplineId2 = DisciplineId.generate();

            mockMvc.perform(put("/api/event-types/{id}", id)
                            .contentType("application/json")
                            .content("""
                                    {"name": "Updated Name", "disciplineIds": ["%s", "%s"]}
                                    """.formatted(disciplineId1.value(), disciplineId2.value())))
                    .andExpect(status().isNoContent());

            verify(eventTypeManagementService).updateEventType(
                    eq(new EventTypeId(id)),
                    argThat(cmd -> cmd.disciplineIds().equals(Set.of(disciplineId1, disciplineId2))));
        }

        @Test
        @DisplayName("should return 409 when ORIS discipline already mapped to another event type on update")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn409WhenOrisDisciplineAlreadyMappedOnUpdate() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new OrisDisciplineAlreadyMappedException())
                    .when(eventTypeManagementService).updateEventType(any(), any());

            mockMvc.perform(put("/api/event-types/{id}", id)
                            .contentType("application/json")
                            .content("""
                                    {"name": "Updated Name"}
                                    """))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 404 when event type not found")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new EventTypeNotFoundException(new EventTypeId(id)))
                    .when(eventTypeManagementService).updateEventType(any(), any());

            mockMvc.perform(put("/api/event-types/{id}", id)
                            .contentType("application/json")
                            .content("""
                                    {"name": "Updated Name"}
                                    """))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when missing authority")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(put("/api/event-types/{id}", UUID.randomUUID())
                            .contentType("application/json")
                            .content("""
                                    {"name": "Updated Name"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/event-types/{id}")
    class DeleteEventTypeTests {

        @Test
        @DisplayName("should return 204 on successful delete")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldDeleteEventType() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/event-types/{id}", id))
                    .andExpect(status().isNoContent());

            verify(eventTypeManagementService).deleteEventType(new EventTypeId(id));
        }

        @Test
        @DisplayName("should return 404 when event type not found")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new EventTypeNotFoundException(new EventTypeId(id)))
                    .when(eventTypeManagementService).deleteEventType(any());

            mockMvc.perform(delete("/api/event-types/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 409 when event type is in use")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn409WhenInUse() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new EventTypeInUseException(new EventTypeId(id), List.of("Krajský přebor", "Letní pohár")))
                    .when(eventTypeManagementService).deleteEventType(any());

            mockMvc.perform(delete("/api/event-types/{id}", id))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail", containsString("Krajský přebor")));
        }

        @Test
        @DisplayName("should return 403 when missing authority")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(delete("/api/event-types/{id}", UUID.randomUUID()))
                    .andExpect(status().isForbidden());
        }
    }
}
