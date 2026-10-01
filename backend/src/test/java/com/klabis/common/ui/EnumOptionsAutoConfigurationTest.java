package com.klabis.common.ui;

import com.klabis.KlabisModuleTest;
import com.klabis.common.WithKlabisMockUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers {@link EnumOptionsAutoConfiguration}: klabisAfford (and friends) must emit inline
 * value-only options for every enum-typed request property automatically, unwrapping
 * JsonNullable/Optional/Collection, skipping null-valued enum constants, preserving declaration
 * order, and always yielding to an explicit override. Test fixture DTOs live in
 * {@code com.klabis.common.ui.infrastructure.restapi} because {@link EnumOptionsAutoConfiguration}
 * only scans record types in packages ending with {@code .infrastructure.restapi}, matching where
 * real request DTOs live.
 */
@KlabisModuleTest
@AutoConfigureMockMvc
@Import(CommonUiExampleControllers.class)
class EnumOptionsAutoConfigurationTest {

    @Autowired
    MockMvc mockMvc;

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("Plain enum property gets auto inline options in declaration order")
    void plainEnumPropertyGetsAutoOptions() throws Exception {
        mockMvc.perform(get("/api/testAutoEnumOptions/plain")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.editPlain.properties[?(@.name == 'status')].options.inline[0]").value("ACTIVE"))
                .andExpect(jsonPath("$._templates.editPlain.properties[?(@.name == 'status')].options.inline[1]").value("INACTIVE"))
                .andExpect(jsonPath("$._templates.editPlain.properties[?(@.name == 'status')].options.inline[2]").value("ARCHIVED"));
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("JsonNullable<enum> property is unwrapped and gets auto inline options")
    void jsonNullableEnumPropertyGetsAutoOptions() throws Exception {
        mockMvc.perform(get("/api/testAutoEnumOptions/nullable")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.editNullable.properties[?(@.name == 'nullableStatus')].options.inline[0]").value("ACTIVE"))
                .andExpect(jsonPath("$._templates.editNullable.properties[?(@.name == 'nullableStatus')].options.inline[1]").value("INACTIVE"))
                .andExpect(jsonPath("$._templates.editNullable.properties[?(@.name == 'nullableStatus')].options.inline[2]").value("ARCHIVED"));
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("Set<enum> property is unwrapped and gets auto inline options")
    void setEnumPropertyGetsAutoOptions() throws Exception {
        mockMvc.perform(get("/api/testAutoEnumOptions/multi")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.editMulti.properties[?(@.name == 'statuses')].options.inline[0]").value("ACTIVE"))
                .andExpect(jsonPath("$._templates.editMulti.properties[?(@.name == 'statuses')].options.inline[1]").value("INACTIVE"))
                .andExpect(jsonPath("$._templates.editMulti.properties[?(@.name == 'statuses')].options.inline[2]").value("ARCHIVED"));
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("Explicit options always win over the auto-detected enum list")
    void explicitOptionsOverrideAutoOptions() throws Exception {
        mockMvc.perform(get("/api/testAutoEnumOptions/explicit")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.editPlain.properties[?(@.name == 'status')].options.inline.length()").value(1))
                .andExpect(jsonPath("$._templates.editPlain.properties[?(@.name == 'status')].options.inline[0]").value("ACTIVE"));
    }

    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("Nullable enum with a null-valued constant skips that constant")
    void nullableEnumWithNullConstantSkipsIt() throws Exception {
        mockMvc.perform(get("/api/testAutoEnumOptions/choice")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.editChoice.properties[?(@.name == 'choice')].options.inline.length()").value(2))
                .andExpect(jsonPath("$._templates.editChoice.properties[?(@.name == 'choice')].options.inline[0]").value("YES"))
                .andExpect(jsonPath("$._templates.editChoice.properties[?(@.name == 'choice')].options.inline[1]").value("NO"));
    }

    /**
     * Two different affordances in the same response both have a property named "status" but on
     * DIFFERENT request DTO classes with different enum types/values — this is the realistic shape
     * of the collision risk called out in EnumOptionsAutoConfiguration's Javadoc. Since
     * auto-detected options are registered per (exact DTO class, property name) directly with
     * HalFormsConfiguration — not through the name-only request-attribute mechanism — neither
     * affordance's options can leak into the other, regardless of registration or rendering order.
     */
    @WithKlabisMockUser(username = "Tester")
    @Test
    @DisplayName("Two affordances with a same-named property on different DTOs each keep their own auto-detected options")
    void autoOptionsDoNotLeakBetweenDifferentDtosSharingAPropertyName() throws Exception {
        mockMvc.perform(get("/api/testAutoEnumOptions/both")
                        .contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.editAuto.properties[?(@.name == 'status')].options.inline.length()").value(3))
                .andExpect(jsonPath("$._templates.editAuto.properties[?(@.name == 'status')].options.inline[0]").value("ACTIVE"))
                .andExpect(jsonPath("$._templates.editOtherAuto.properties[?(@.name == 'status')].options.inline.length()").value(2))
                .andExpect(jsonPath("$._templates.editOtherAuto.properties[?(@.name == 'status')].options.inline[0]").value("YES"))
                .andExpect(jsonPath("$._templates.editOtherAuto.properties[?(@.name == 'status')].options.inline[1]").value("NO"));
    }
}
