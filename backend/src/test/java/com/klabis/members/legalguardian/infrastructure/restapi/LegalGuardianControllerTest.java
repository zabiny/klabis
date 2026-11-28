package com.klabis.members.legalguardian.infrastructure.restapi;

import com.klabis.members.MembersWebMvcTest;
import com.klabis.common.SelfGrants;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.common.security.JwtParams;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static com.klabis.common.security.KlabisMvcRequestBuilders.klabisAuthentication;
import static com.klabis.common.security.KlabisMvcRequestBuilders.klabisMemberWithSelfGrants;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
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
        @WithKlabisMockUser(userId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ}, selfGrants = SelfGrants.NON_MEMBER)
        void returnsProfileForOwner() throws Exception {
            when(legalGuardianService.get(GUARDIAN_USER_ID)).thenReturn(profile());

            mockMvc.perform(get("/api/legal-guardians/{id}", GUARDIAN_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.updateLegalGuardian").exists());
        }

        @Test
        @DisplayName("includes registerAsMember link for MEMBERS:MANAGE")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void includesRegisterAsMemberLinkForManager() throws Exception {
            when(legalGuardianService.get(GUARDIAN_USER_ID)).thenReturn(profile());

            mockMvc.perform(get("/api/legal-guardians/{id}", GUARDIAN_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.registerAsMember.href", endsWith("/api/members")));
        }

        @Test
        @DisplayName("omits registerAsMember link for the guardian themself")
        @WithKlabisMockUser(userId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ}, selfGrants = SelfGrants.NON_MEMBER)
        void omitsRegisterAsMemberLinkForOwner() throws Exception {
            when(legalGuardianService.get(GUARDIAN_USER_ID)).thenReturn(profile());

            mockMvc.perform(get("/api/legal-guardians/{id}", GUARDIAN_ID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.registerAsMember").doesNotExist());
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
        @WithKlabisMockUser(userId = GUARDIAN_ID, authorities = {Authority.MEMBERS_READ}, selfGrants = SelfGrants.NON_MEMBER)
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

        static Stream<Arguments> invalidBodies() {
            return Stream.of(
                    argumentSet("blank e-mail", "{\"email\":\"\"}"),
                    argumentSet("explicit null e-mail",
                            "{\"email\":null,\"firstName\":\"Lenka\",\"lastName\":\"Kratochvílová\",\"phone\":\"+420777888999\"}"),
                    argumentSet("explicit null phone",
                            "{\"email\":\"a@b.cz\",\"firstName\":\"Lenka\",\"lastName\":\"Kratochvílová\",\"phone\":null}"));
        }

        @ParameterizedTest
        @MethodSource("invalidBodies")
        @DisplayName("returns 400 and saves nothing for an invalid body")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void returns400AndSavesNothingForInvalidBody(String body) throws Exception {
            mockMvc.perform(patch("/api/legal-guardians/{id}", GUARDIAN_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(legalGuardianService);
        }
    }

    static Stream<Arguments> forbiddenRequests() {
        RequestPostProcessor stranger = klabisAuthentication(
                JwtParams.jwtTokenParams("ZBM8001", UUID.randomUUID()).withAuthorities(Authority.MEMBERS_READ));
        RequestPostProcessor otherMember = klabisMemberWithSelfGrants(
                UUID.fromString("33333333-3333-3333-3333-333333333333"), Authority.MEMBERS_READ);
        return Stream.of(
                argumentSet("GET by a stranger", HttpMethod.GET, stranger),
                argumentSet("GET by a member reading another guardian", HttpMethod.GET, otherMember),
                argumentSet("PATCH by a stranger", HttpMethod.PATCH, stranger),
                argumentSet("PATCH by a member updating another guardian", HttpMethod.PATCH, otherMember));
    }

    @ParameterizedTest
    @MethodSource("forbiddenRequests")
    @DisplayName("returns 403 and reaches no service without access to the guardian")
    void returns403WithoutAccessToTheGuardian(HttpMethod method, RequestPostProcessor user) throws Exception {
        mockMvc.perform(request(method, "/api/legal-guardians/{id}", GUARDIAN_ID)
                        .with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content("{\"email\":\"petr@example.com\",\"phone\":\"+420777888999\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(legalGuardianService);
    }
}
