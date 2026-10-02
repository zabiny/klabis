package com.klabis.oris.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.oris.OrisWebMvcTest;
import com.klabis.common.settings.OrisClubKeyManagementPort;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Design.md D9/D10: the club key is never disclosed, and the resource carries exactly one of
 * the 'set'/'clear' affordances depending on whether a key is currently held.
 */
@DisplayName("OrisClubKeyController")
@OrisWebMvcTest
class OrisClubKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrisClubKeyManagementPort orisClubKeyManagementPort;

    @Nested
    @DisplayName("GET /api/oris/club-key")
    class GetClubKeyState {

        @Test
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        @DisplayName("no key held -> isSet false, only the 'set' affordance")
        void noKeyHeld_reportsUnsetWithSetAffordanceOnly() throws Exception {
            when(orisClubKeyManagementPort.isSet()).thenReturn(false);

            mockMvc.perform(get("/api/oris/club-key").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSet").value(false))
                    .andExpect(jsonPath("$._templates.setClubKey").exists())
                    .andExpect(jsonPath("$._templates.clearClubKey").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        @DisplayName("key held -> isSet true, only the 'clear' affordance")
        void keyHeld_reportsSetWithClearAffordanceOnly() throws Exception {
            when(orisClubKeyManagementPort.isSet()).thenReturn(true);

            mockMvc.perform(get("/api/oris/club-key").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSet").value(true))
                    .andExpect(jsonPath("$._templates.clearClubKey").exists())
                    .andExpect(jsonPath("$._templates.setClubKey").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        @DisplayName("response never carries the key value itself")
        void neverDisclosesTheKeyValue() throws Exception {
            when(orisClubKeyManagementPort.isSet()).thenReturn(true);

            mockMvc.perform(get("/api/oris/club-key").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.clubKey").doesNotExist())
                    .andExpect(jsonPath("$.value").doesNotExist());
        }

        @Test
        @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
        @DisplayName("user without SYNC:MANAGE -> 403")
        void withoutSyncManageAuthority_returns403() throws Exception {
            when(orisClubKeyManagementPort.isSet()).thenReturn(false);

            mockMvc.perform(get("/api/oris/club-key").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("unauthenticated -> 401")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/oris/club-key").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/oris/club-key")
    class SetClubKey {

        @Test
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        @DisplayName("valid key -> stored, isSet true in response")
        void validKey_isStored() throws Exception {
            when(orisClubKeyManagementPort.isSet()).thenReturn(true);

            mockMvc.perform(put("/api/oris/club-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaTypes.HAL_FORMS_JSON)
                            .content("""
                                    {"clubKey": "secret-value"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSet").value(true));

            verify(orisClubKeyManagementPort).store("secret-value");
        }

        @Test
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        @DisplayName("blank key -> 400, refused before reaching the port")
        void blankKey_isRefused() throws Exception {
            mockMvc.perform(put("/api/oris/club-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaTypes.HAL_FORMS_JSON)
                            .content("""
                                    {"clubKey": "   "}
                                    """))
                    .andExpect(status().isBadRequest());

            Mockito.verifyNoInteractions(orisClubKeyManagementPort);
        }

        @Test
        @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
        @DisplayName("user without SYNC:MANAGE -> 403")
        void withoutSyncManageAuthority_returns403() throws Exception {
            mockMvc.perform(put("/api/oris/club-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaTypes.HAL_FORMS_JSON)
                            .content("""
                                    {"clubKey": "secret-value"}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/oris/club-key")
    class ClearClubKey {

        @Test
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        @DisplayName("key held -> discarded, 204")
        void keyHeld_isDiscarded() throws Exception {
            mockMvc.perform(delete("/api/oris/club-key").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNoContent());

            verify(orisClubKeyManagementPort).clear();
        }

        @Test
        @WithKlabisMockUser(authorities = Authority.SYNC_MANAGE)
        @DisplayName("no key held -> still succeeds without error")
        void noKeyHeld_stillSucceeds() throws Exception {
            mockMvc.perform(delete("/api/oris/club-key").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isNoContent());

            verify(orisClubKeyManagementPort).clear();
        }

        @Test
        @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
        @DisplayName("user without SYNC:MANAGE -> 403")
        void withoutSyncManageAuthority_returns403() throws Exception {
            mockMvc.perform(delete("/api/oris/club-key").accept(MediaTypes.HAL_FORMS_JSON))
                    .andExpect(status().isForbidden());

            Mockito.verifyNoInteractions(orisClubKeyManagementPort);
        }
    }
}
