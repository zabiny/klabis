package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.Authority;
import com.klabis.members.MemberId;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.MemberAccountActivationPort;
import com.klabis.members.application.MemberNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/members/{id}/account-activation")
@MembersWebMvcTest
class MemberAccountActivationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberAccountActivationPort accountActivationPort;

    private final UUID memberId = UUID.randomUUID();

    @Test
    @DisplayName("returns 204 and sends the activation for the member")
    @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
    void sendsActivation() throws Exception {
        mockMvc.perform(post("/api/members/{id}/account-activation", memberId).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isNoContent());

        verify(accountActivationPort).sendAccountActivation(new MemberId(memberId));
    }

    @Test
    @DisplayName("returns 400 when the activation is not available for the member")
    @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
    void rejectsUnavailableActivation() throws Exception {
        doThrow(new BusinessRuleViolationException("not available") {
        }).when(accountActivationPort).sendAccountActivation(new MemberId(memberId));

        mockMvc.perform(post("/api/members/{id}/account-activation", memberId).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("returns 404 for an unknown member")
    @WithKlabisMockUser(authorities = Authority.MEMBERS_MANAGE)
    void unknownMember() throws Exception {
        doThrow(new MemberNotFoundException(new MemberId(memberId)))
                .when(accountActivationPort).sendAccountActivation(new MemberId(memberId));

        mockMvc.perform(post("/api/members/{id}/account-activation", memberId).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("returns 403 without MEMBERS:MANAGE")
    @WithKlabisMockUser(authorities = Authority.MEMBERS_READ)
    void forbiddenWithoutManage() throws Exception {
        mockMvc.perform(post("/api/members/{id}/account-activation", memberId).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("returns 401 when unauthenticated")
    void unauthenticated() throws Exception {
        mockMvc.perform(post("/api/members/{id}/account-activation", memberId).accept(MediaTypes.HAL_FORMS_JSON))
                .andExpect(status().isUnauthorized());
    }
}
