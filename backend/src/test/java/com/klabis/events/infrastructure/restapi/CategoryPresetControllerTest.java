package com.klabis.events.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.events.CategoryPresetId;
import com.klabis.events.application.CategoryPresetManagementPort;
import com.klabis.events.application.CategoryPresetNotFoundException;
import com.klabis.events.domain.CategoryPreset;
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
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("CategoryPresetController API tests")
@WebMvcTest(controllers = {CategoryPresetController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class CategoryPresetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryPresetManagementPort categoryPresetManagementService;

    @Nested
    @DisplayName("GET /api/category-presets")
    class ListPresetsTests {

        @Test
        @DisplayName("should return 200 with list of presets")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturnListOfPresets() throws Exception {
            CategoryPreset preset = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Sprint Cup", List.of("M21", "W35")));
            when(categoryPresetManagementService.listAll()).thenReturn(List.of(preset));

            mockMvc.perform(get("/api/category-presets").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.categoryPresetDtoList[0].name").value("Sprint Cup"))
                    .andExpect(jsonPath("$._embedded.categoryPresetDtoList[0].categories[0]").value("M21"))
                    .andExpect(jsonPath("$._embedded.categoryPresetDtoList[0].categories[1]").value("W35"));
        }

        @Test
        @DisplayName("should expose createCategoryPreset template name on collection self link")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldExposeCreateCategoryPresetTemplateName() throws Exception {
            when(categoryPresetManagementService.listAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/category-presets").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.createCategoryPreset").exists())
                    .andExpect(jsonPath("$._templates.createCategoryPreset.method").value("POST"));
        }

        @Test
        @DisplayName("should return 200 with empty collection when no presets")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturnEmptyCollection() throws Exception {
            when(categoryPresetManagementService.listAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/category-presets").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/category-presets").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 when missing EVENTS:MANAGE authority")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(get("/api/category-presets").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/category-presets/{id}")
    class GetPresetTests {

        @Test
        @DisplayName("should return 200 with preset details")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturnPresetDetails() throws Exception {
            UUID id = UUID.randomUUID();
            CategoryPreset preset = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Forest Run", List.of("M21")));
            when(categoryPresetManagementService.getPreset(any(CategoryPresetId.class))).thenReturn(preset);

            mockMvc.perform(get("/api/category-presets/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Forest Run"))
                    .andExpect(jsonPath("$.categories[0]").value("M21"));
        }

        @Test
        @DisplayName("should return 404 when preset not found")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(categoryPresetManagementService.getPreset(any(CategoryPresetId.class)))
                    .thenThrow(new CategoryPresetNotFoundException(new CategoryPresetId(id)));

            mockMvc.perform(get("/api/category-presets/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should expose updateCategoryPreset and deleteCategoryPreset template names")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldExposeCorrectTemplateNames() throws Exception {
            UUID id = UUID.randomUUID();
            CategoryPreset preset = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Forest Run", List.of("M21")));
            when(categoryPresetManagementService.getPreset(any(CategoryPresetId.class))).thenReturn(preset);

            mockMvc.perform(get("/api/category-presets/{id}", id).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateCategoryPreset.method").value("PATCH"))
                    .andExpect(jsonPath("$._templates.deleteCategoryPreset.method").value("DELETE"));
        }
    }

    @Nested
    @DisplayName("POST /api/category-presets")
    class CreatePresetTests {

        @Test
        @DisplayName("should return 201 with Location header")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldCreatePreset() throws Exception {
            CategoryPreset created = CategoryPreset.create(new CategoryPreset.CreateCategoryPreset("Sprint Cup", List.of("M21")));
            when(categoryPresetManagementService.createPreset(any())).thenReturn(created);

            String body = """
                    {"name": "Sprint Cup", "categories": ["M21"]}
                    """;

            mockMvc.perform(post("/api/category-presets")
                            .contentType("application/json")
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/api/category-presets/")));
        }

        @Test
        @DisplayName("should return 400 when name is missing")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn400WhenNameMissing() throws Exception {
            mockMvc.perform(post("/api/category-presets")
                            .contentType("application/json")
                            .content("""
                                    {"categories": ["M21"]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when missing authority")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(post("/api/category-presets")
                            .contentType("application/json")
                            .content("""
                                    {"name": "Sprint Cup", "categories": []}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/category-presets/{id}")
    class UpdatePresetTests {

        @Test
        @DisplayName("should return 204 on successful update")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldUpdatePreset() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(patch("/api/category-presets/{id}", id)
                            .contentType("application/json")
                            .content("""
                                    {"name": "Updated Name", "categories": ["W35"]}
                                    """))
                    .andExpect(status().isNoContent());

            verify(categoryPresetManagementService).updatePreset(eq(new CategoryPresetId(id)), any());
        }

        @Test
        @DisplayName("should return 404 when preset not found")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new CategoryPresetNotFoundException(new CategoryPresetId(id)))
                    .when(categoryPresetManagementService).updatePreset(any(), any());

            mockMvc.perform(patch("/api/category-presets/{id}", id)
                            .contentType("application/json")
                            .content("""
                                    {"name": "Updated Name", "categories": []}
                                    """))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/category-presets/{id}")
    class DeletePresetTests {

        @Test
        @DisplayName("should return 204 on successful delete")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldDeletePreset() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(delete("/api/category-presets/{id}", id))
                    .andExpect(status().isNoContent());

            verify(categoryPresetManagementService).deletePreset(new CategoryPresetId(id));
        }

        @Test
        @DisplayName("should return 404 when preset not found")
        @WithKlabisMockUser(authorities = {Authority.EVENTS_MANAGE})
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new CategoryPresetNotFoundException(new CategoryPresetId(id)))
                    .when(categoryPresetManagementService).deletePreset(any());

            mockMvc.perform(delete("/api/category-presets/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when missing authority")
        @WithKlabisMockUser(authorities = {})
        void shouldReturn403WhenMissingAuthority() throws Exception {
            mockMvc.perform(delete("/api/category-presets/{id}", UUID.randomUUID()))
                    .andExpect(status().isForbidden());
        }
    }
}
