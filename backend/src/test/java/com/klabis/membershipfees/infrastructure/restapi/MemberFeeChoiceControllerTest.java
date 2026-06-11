package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.WithPostprocessors;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.MemberChoicePort;
import com.klabis.membershipfees.domain.VotingClosedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("MemberFeeChoiceController API tests")
@WebMvcTest(controllers = MemberFeeChoiceController.class)
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
@WithPostprocessors
class MemberFeeChoiceControllerTest {

    private static final String MEMBER_ID_STR = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID MEMBER_UUID = UUID.fromString(MEMBER_ID_STR);
    private static final String OTHER_MEMBER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID LEVEL_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final int YEAR = 2026;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberChoicePort memberChoicePort;

    @Nested
    @DisplayName("GET /api/members/{memberId}/fee-choice/{year}")
    class GetChoiceTests {

        @Test
        @DisplayName("should return 200 with current choice when member has chosen")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturnCurrentChoice() throws Exception {
            when(memberChoicePort.getCurrentChoice(any(), eq(YEAR)))
                    .thenReturn(Optional.of(new MembershipFeeGroupId(GROUP_UUID)));
            when(memberChoicePort.getRecommendedLevelForYear(any(), eq(YEAR)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memberId").value(MEMBER_ID_STR))
                    .andExpect(jsonPath("$.year").value(YEAR))
                    .andExpect(jsonPath("$.currentGroupId").value(GROUP_UUID.toString()));
        }

        @Test
        @DisplayName("should return recommended default when no current choice")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturnRecommendedDefault() throws Exception {
            when(memberChoicePort.getCurrentChoice(any(), eq(YEAR)))
                    .thenReturn(Optional.empty());
            when(memberChoicePort.getRecommendedLevelForYear(any(), eq(YEAR)))
                    .thenReturn(Optional.of(new MembershipFeeTierId(LEVEL_UUID)));

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentGroupId").doesNotExist())
                    .andExpect(jsonPath("$.recommendedLevelId").value(LEVEL_UUID.toString()));
        }

        @Test
        @DisplayName("should return 403 when member accesses another member's choice")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenAccessingOtherMember() throws Exception {
            mockMvc.perform(
                            get("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should include choose and remove affordances")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldIncludeAffordances() throws Exception {
            when(memberChoicePort.getCurrentChoice(any(), eq(YEAR))).thenReturn(Optional.empty());
            when(memberChoicePort.getRecommendedLevelForYear(any(), eq(YEAR))).thenReturn(Optional.empty());

            mockMvc.perform(
                            get("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.chooseLevel").exists())
                    .andExpect(jsonPath("$._templates.removeChoice").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/members/{memberId}/fee-choice/{year}")
    class ChooseLevelTests {

        @Test
        @DisplayName("should return 204 when choice is successful")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturn204OnSuccess() throws Exception {
            doNothing().when(memberChoicePort).chooseFeeLevel(any());

            mockMvc.perform(
                            post("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"membershipFeeGroupId": "%s"}
                                            """.formatted(GROUP_UUID)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when voting is closed")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturn400WhenVotingClosed() throws Exception {
            doThrow(new VotingClosedException()).when(memberChoicePort).chooseFeeLevel(any());

            mockMvc.perform(
                            post("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"membershipFeeGroupId": "%s"}
                                            """.formatted(GROUP_UUID)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when membershipFeeGroupId is missing")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturn400WhenGroupIdMissing() throws Exception {
            mockMvc.perform(
                            post("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when member acts as another member")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenOtherMember() throws Exception {
            mockMvc.perform(
                            post("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"membershipFeeGroupId": "%s"}
                                            """.formatted(GROUP_UUID)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/members/{memberId}/fee-choice/{year}")
    class RemoveChoiceTests {

        @Test
        @DisplayName("should return 204 on successful removal")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturn204OnSuccess() throws Exception {
            doNothing().when(memberChoicePort).removeFeeChoice(any(), eq(YEAR));

            mockMvc.perform(
                            delete("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 400 when voting is closed")
        @WithKlabisMockUser(memberId = MEMBER_ID_STR)
        void shouldReturn400WhenVotingClosed() throws Exception {
            doThrow(new VotingClosedException()).when(memberChoicePort).removeFeeChoice(any(), eq(YEAR));

            mockMvc.perform(
                            delete("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 403 when member acts as another member")
        @WithKlabisMockUser(memberId = OTHER_MEMBER_ID)
        void shouldReturn403WhenOtherMember() throws Exception {
            mockMvc.perform(
                            delete("/api/members/{memberId}/fee-choice/{year}", MEMBER_UUID, YEAR)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(status().isForbidden());
        }
    }
}
