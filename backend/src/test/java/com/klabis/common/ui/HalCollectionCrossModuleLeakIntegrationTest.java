package com.klabis.common.ui;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards against {@code HalResponseBodyAdvice.wrapCollection} matching collection-level
 * {@code RepresentationModelProcessor}s from unrelated modules. A plain {@code List<XxxSummaryResponse>}
 * endpoint must only pick up the collection-level postprocessor registered for its own DTO type —
 * not every {@code RepresentationModelProcessor<CollectionModel<EntityModel<?>>>} in the context.
 * <p>
 * Requires the full application context ({@code @SpringBootTest}); a {@code @WebMvcTest} slice only
 * registers the postprocessors of the controller under test, so it cannot reproduce the leak.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("HalResponseBodyAdvice collection wrapping — no cross-module postprocessor leak")
class HalCollectionCrossModuleLeakIntegrationTest {

    private static final String ADMIN_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_MANAGE})
    @DisplayName("GET /api/legal-guardian-groups (empty) exposes no create affordance of another module and a self link")
    void legalGuardianGroupsCollectionCarriesOnlyItsOwnAffordanceWhenEmpty() throws Exception {
        mockMvc.perform(get("/api/legal-guardian-groups").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.createLegalGuardianGroup").doesNotExist())
                .andExpect(jsonPath("$._templates.createEventType").doesNotExist())
                .andExpect(jsonPath("$._templates.createCalendarItem").doesNotExist())
                .andExpect(jsonPath("$._templates.createGroup").doesNotExist())
                .andExpect(jsonPath("$._templates.createTrainingGroup").doesNotExist())
                .andExpect(jsonPath("$._templates.createCategoryPreset").doesNotExist())
                .andExpect(jsonPath("$._links.self").exists())
                .andExpect(jsonPath("$._links.next").doesNotExist())
                .andExpect(jsonPath("$._links.prev").doesNotExist());
    }

    @Test
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_MANAGE, Authority.CALENDAR_MANAGE})
    @DisplayName("GET /api/legal-guardian-groups after a calendar-items call in the same thread stays isolated")
    void legalGuardianGroupsCollectionStaysIsolatedAfterAnotherCollectionEndpoint() throws Exception {
        mockMvc.perform(get("/api/calendar-items").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/legal-guardian-groups").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.createLegalGuardianGroup").doesNotExist())
                .andExpect(jsonPath("$._templates.createCalendarItem").doesNotExist())
                .andExpect(jsonPath("$._templates.createEventType").doesNotExist())
                .andExpect(jsonPath("$._templates.createGroup").doesNotExist())
                .andExpect(jsonPath("$._templates.createTrainingGroup").doesNotExist())
                .andExpect(jsonPath("$._templates.createCategoryPreset").doesNotExist())
                .andExpect(jsonPath("$._links.next").doesNotExist())
                .andExpect(jsonPath("$._links.prev").doesNotExist());
    }
}
