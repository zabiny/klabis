package com.klabis.members.legalguardian.infrastructure.restapi;

import com.klabis.members.MembersWebMvcTest;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.PersonName;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.legalguardian.application.LegalGuardianNotFoundException;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardian.application.LegalGuardianProfile;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("LegalGuardianController API tests")
@MembersWebMvcTest
class LegalGuardianControllerTest {

    private static final String GUARDIAN_ID = "22222222-2222-2222-2222-222222222222";
    private static final UserId GUARDIAN_USER_ID = new UserId(UUID.fromString(GUARDIAN_ID));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LegalGuardianPort legalGuardianService;

    private static LegalGuardianProfile profile() {
        LegalGuardian guardian = LegalGuardian.reconstruct(GUARDIAN_USER_ID, PersonName.of("Petr", "Rodič"),
                EmailAddress.of("petr@example.com"), PhoneNumber.of("+420123456789"), null);
        return new LegalGuardianProfile(guardian, "EXT0001");
    }

    @Nested
    @DisplayName("GET /api/legal-guardians/{userId}")
    class GetGuardian {

        @Test
        @DisplayName("returns profile with self link and update affordance for MEMBERS:MANAGE")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void returnsProfileForManager() throws Exception {
            when(legalGuardianService.get(GUARDIAN_USER_ID)).thenReturn(profile());

            mockMvc.perform(get("/api/legal-guardians/{id}", GUARDIAN_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(GUARDIAN_ID))
                    .andExpect(jsonPath("$.loginName").value("EXT0001"))
                    .andExpect(jsonPath("$.firstName").value("Petr"))
                    .andExpect(jsonPath("$.email").value("petr@example.com"))
                    .andExpect(jsonPath("$._links.self.href", endsWith("/api/legal-guardians/" + GUARDIAN_ID)))
                    .andExpect(jsonPath("$._templates.updateLegalGuardian").exists())
                    .andExpect(jsonPath("$._templates.updateLegalGuardian.properties[?(@.name=='email')].min").value(org.hamcrest.Matchers.contains(1)))
                    .andExpect(jsonPath("$._templates.updateLegalGuardian.properties[?(@.name=='phone')].min").value(org.hamcrest.Matchers.contains(1)));
        }

        @Test
        @DisplayName("returns profile for the guardian themself")
        @WithKlabisMockUser(userId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ})
        void returnsProfileForOwner() throws Exception {
            when(legalGuardianService.get(GUARDIAN_USER_ID)).thenReturn(profile());

            mockMvc.perform(get("/api/legal-guardians/{id}", GUARDIAN_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateLegalGuardian").exists());
        }

        @Test
        @DisplayName("returns 403 for a stranger")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
        void forbiddenForStranger() throws Exception {
            mockMvc.perform(get("/api/legal-guardians/{id}", GUARDIAN_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns 401 when unauthenticated")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/api/legal-guardians/{id}", GUARDIAN_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 404 for unknown guardian")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void notFound() throws Exception {
            when(legalGuardianService.get(GUARDIAN_USER_ID)).thenThrow(new LegalGuardianNotFoundException(GUARDIAN_USER_ID));

            mockMvc.perform(get("/api/legal-guardians/{id}", GUARDIAN_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/legal-guardians/{userId}")
    class UpdateGuardian {

        @Test
        @DisplayName("updates contacts and returns 204 for the guardian themself")
        @WithKlabisMockUser(userId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ})
        void updatesOwnContacts() throws Exception {
            when(legalGuardianService.update(any(), any())).thenReturn(profile());

            mockMvc.perform(patch("/api/legal-guardians/{id}", GUARDIAN_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"petr@example.com\",\"phone\":\"+420777888999\"}"))
                    .andExpect(status().isNoContent());

            ArgumentCaptor<LegalGuardian.UpdateLegalGuardian> captor = ArgumentCaptor.forClass(LegalGuardian.UpdateLegalGuardian.class);
            verify(legalGuardianService).update(eq(GUARDIAN_USER_ID), captor.capture());
            assertThat(captor.getValue().phone()).isEqualTo("+420777888999");
            assertThat(captor.getValue().email()).isEqualTo("petr@example.com");
        }

        @Test
        @DisplayName("returns 403 for a stranger")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_READ})
        void forbiddenForStranger() throws Exception {
            mockMvc.perform(patch("/api/legal-guardians/{id}", GUARDIAN_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"petr@example.com\",\"phone\":\"+420777888999\"}"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(legalGuardianService);
        }

        @Test
        @DisplayName("returns 400 when e-mail is blank")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void rejectsBlankEmail() throws Exception {
            mockMvc.perform(patch("/api/legal-guardians/{id}", GUARDIAN_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }
    
        @Test
        @DisplayName("returns 400 and saves nothing when e-mail is explicitly null")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void rejectsNullEmail() throws Exception {
            mockMvc.perform(patch("/api/legal-guardians/{id}", GUARDIAN_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":null,\"firstName\":\"Lenka\",\"lastName\":\"Kratochvílová\",\"phone\":\"+420777888999\"}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(legalGuardianService);
        }

        @Test
        @DisplayName("returns 400 and saves nothing when phone is explicitly null")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void rejectsNullPhone() throws Exception {
            mockMvc.perform(patch("/api/legal-guardians/{id}", GUARDIAN_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"a@b.cz\",\"firstName\":\"Lenka\",\"lastName\":\"Kratochvílová\",\"phone\":null}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(legalGuardianService);
        }
    }
}
