package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserService;
import com.klabis.usergroups.UserGroupId;
import com.klabis.usergroups.application.GroupManagementPort;
import com.klabis.usergroups.domain.AgeRange;
import com.klabis.usergroups.domain.GroupMembership;
import com.klabis.members.MemberId;
import com.klabis.usergroups.domain.TrainingGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TrainingGroupController API tests")
@WebMvcTest(controllers = TrainingGroupController.class)
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
class TrainingGroupControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UserGroupId GROUP_ID = new UserGroupId(GROUP_UUID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GroupManagementPort groupManagementService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private TrainingGroup buildTrainingGroup(UUID groupUuid, String name, AgeRange ageRange, String ownerUuidStr) {
        MemberId owner = new MemberId(UUID.fromString(ownerUuidStr));
        return TrainingGroup.reconstruct(
                new UserGroupId(groupUuid), name, Set.of(owner), Set.of(), ageRange, null);
    }

    @Nested
    @DisplayName("POST /api/training-groups")
    class CreateTrainingGroupTests {

        @Test
        @DisplayName("should return 403 when user lacks GROUPS:TRAINING authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingTrainingAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/training-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Juniors", "minAge": 10, "maxAge": 18}
                                            """)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 201 with Location header when user has GROUPS:TRAINING")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldCreateTrainingGroupAndReturn201() throws Exception {
            TrainingGroup created = buildTrainingGroup(GROUP_UUID, "Juniors", new AgeRange(10, 18), MEMBER_ID);
            when(groupManagementService.createTrainingGroup(any(TrainingGroup.CreateTrainingGroup.class)))
                    .thenReturn(created);

            mockMvc.perform(
                            post("/api/training-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Juniors", "minAge": 10, "maxAge": 18}
                                            """)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/training-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Juniors", "minAge": 10, "maxAge": 18}
                                            """)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/training-groups")
    class ListTrainingGroupsTests {

        @Test
        @DisplayName("should return 403 when user lacks GROUPS:TRAINING authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingTrainingAuthority() throws Exception {
            mockMvc.perform(
                            get("/api/training-groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 200 with collection when user has GROUPS:TRAINING")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldReturnCollectionWhenAuthorized() throws Exception {
            TrainingGroup group = buildTrainingGroup(GROUP_UUID, "Juniors", new AgeRange(10, 18), MEMBER_ID);
            when(groupManagementService.listTrainingGroups()).thenReturn(List.of(group));

            mockMvc.perform(
                            get("/api/training-groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            get("/api/training-groups")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/training-groups/{id}")
    class GetTrainingGroupTests {

        @Test
        @DisplayName("should return 200 with group details when user has GROUPS:TRAINING")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldReturnGroupDetails() throws Exception {
            TrainingGroup group = buildTrainingGroup(GROUP_UUID, "Juniors", new AgeRange(10, 18), MEMBER_ID);
            when(groupManagementService.getTrainingGroup(any(UserGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/training-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Juniors"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.minAge").value(10))
                    .andExpect(jsonPath("$.maxAge").value(18))
                    .andExpect(jsonPath("$.owners").isArray())
                    .andExpect(jsonPath("$.members").isArray());
        }

        @Test
        @DisplayName("should return 403 when user lacks GROUPS:TRAINING authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingTrainingAuthority() throws Exception {
            mockMvc.perform(
                            get("/api/training-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            get("/api/training-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }
    }
}
