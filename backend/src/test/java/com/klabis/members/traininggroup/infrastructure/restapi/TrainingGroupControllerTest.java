package com.klabis.members.traininggroup.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.ui.HalFormsSupport;
import com.klabis.common.usergroup.CannotRemoveLastOwnerException;
import com.klabis.common.usergroup.DirectMemberAdditionNotAllowedException;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.members.traininggroup.application.TrainingGroupManagementPort;
import com.klabis.members.traininggroup.domain.AgeRange;
import com.klabis.members.traininggroup.domain.TrainingGroup;
import com.klabis.members.traininggroup.domain.TrainingGroupId;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TrainingGroupController API tests")
@WebMvcTest(controllers = {TrainingGroupController.class})
@Import({EncryptionConfiguration.class, HalFormsSupport.class})
class TrainingGroupControllerTest {

    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String TRAINER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrainingGroupManagementPort trainingGroupManagementService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private TrainingGroup buildTrainingGroup(UUID groupUuid, String name, AgeRange ageRange, String trainerUuidStr) {
        MemberId trainer = new MemberId(UUID.fromString(trainerUuidStr));
        return TrainingGroup.reconstruct(
                new TrainingGroupId(groupUuid), name, Set.of(trainer), Set.of(), ageRange, null);
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
                                            {"name": "Juniors", "trainerId": "%s", "minAge": 10, "maxAge": 18}
                                            """.formatted(TRAINER_ID))
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 201 with Location header when user has GROUPS:TRAINING")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldCreateTrainingGroupAndReturn201() throws Exception {
            TrainingGroup created = buildTrainingGroup(GROUP_UUID, "Juniors", new AgeRange(10, 18), TRAINER_ID);
            when(trainingGroupManagementService.createTrainingGroup(any(TrainingGroup.CreateTrainingGroup.class)))
                    .thenReturn(created);

            mockMvc.perform(
                            post("/api/training-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Juniors", "trainerId": "%s", "minAge": 10, "maxAge": 18}
                                            """.formatted(TRAINER_ID))
                    )
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"));
        }

        @Test
        @DisplayName("should return 400 when trainerId is missing")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldReturn400WhenTrainerIdMissing() throws Exception {
            mockMvc.perform(
                            post("/api/training-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Juniors", "minAge": 10, "maxAge": 18}
                                            """)
                    )
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(
                            post("/api/training-groups")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Juniors", "trainerId": "%s", "minAge": 10, "maxAge": 18}
                                            """.formatted(TRAINER_ID))
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
            TrainingGroup group = buildTrainingGroup(GROUP_UUID, "Juniors", new AgeRange(10, 18), TRAINER_ID);
            when(trainingGroupManagementService.listTrainingGroups()).thenReturn(List.of(group));

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
        @DisplayName("should return 200 with full group details when user has GROUPS:TRAINING")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ, Authority.GROUPS_TRAINING})
        void shouldReturnFullGroupDetailsWithTrainingAuthority() throws Exception {
            TrainingGroup group = buildTrainingGroup(GROUP_UUID, "Juniors", new AgeRange(10, 18), TRAINER_ID);
            when(trainingGroupManagementService.getTrainingGroup(any(TrainingGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/training-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Juniors"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.minAge").value(10))
                    .andExpect(jsonPath("$.maxAge").value(18))
                    .andExpect(jsonPath("$.trainers").isArray())
                    .andExpect(jsonPath("$.members").isArray())
                    .andExpect(jsonPath("$.owners").doesNotExist());
        }

        @Test
        @DisplayName("should return 200 with limited response when user lacks GROUPS:TRAINING")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ})
        void shouldReturnLimitedGroupDetailsWithoutTrainingAuthority() throws Exception {
            TrainingGroup group = buildTrainingGroup(GROUP_UUID, "Juniors", new AgeRange(10, 18), TRAINER_ID);
            when(trainingGroupManagementService.getTrainingGroup(any(TrainingGroupId.class))).thenReturn(group);

            mockMvc.perform(
                            get("/api/training-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Juniors"))
                    .andExpect(jsonPath("$.trainers").isArray())
                    .andExpect(jsonPath("$.minAge").doesNotExist())
                    .andExpect(jsonPath("$.maxAge").doesNotExist())
                    .andExpect(jsonPath("$.members").doesNotExist())
                    .andExpect(jsonPath("$.owners").doesNotExist());
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

    @Nested
    @DisplayName("PATCH /api/training-groups/{id}")
    class UpdateTrainingGroupTests {

        @Test
        @DisplayName("should return 204 when updating name only")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldReturn204WhenUpdatingNameOnly() throws Exception {
            TrainingGroup group = buildTrainingGroup(GROUP_UUID, "Updated", new AgeRange(10, 18), TRAINER_ID);
            when(trainingGroupManagementService.updateTrainingGroup(any(TrainingGroupId.class), any()))
                    .thenReturn(group);

            mockMvc.perform(
                            patch("/api/training-groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Updated"}
                                            """)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 204 when name is updated and trainers field is explicitly null (HAL forms sends null for untouched list)")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldReturn204WhenUpdatingNameWithExplicitNullTrainers() throws Exception {
            TrainingGroup group = buildTrainingGroup(GROUP_UUID, "Updated", new AgeRange(10, 18), TRAINER_ID);
            when(trainingGroupManagementService.updateTrainingGroup(any(TrainingGroupId.class), any()))
                    .thenReturn(group);

            mockMvc.perform(
                            patch("/api/training-groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Updated", "trainers": null}
                                            """)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when user lacks GROUPS:TRAINING authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingTrainingAuthority() throws Exception {
            mockMvc.perform(
                            patch("/api/training-groups/{id}", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"name": "Updated"}
                                            """)
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/training-groups/{id}/members")
    class AddTrainingGroupMemberTests {

        @Test
        @DisplayName("should return 422 when domain rejects direct member addition")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldReturn422WhenDirectMemberAdditionNotAllowed() throws Exception {
            doThrow(new DirectMemberAdditionNotAllowedException())
                    .when(trainingGroupManagementService).addMemberToTrainingGroup(any(TrainingGroupId.class), any(MemberId.class));

            mockMvc.perform(
                            post("/api/training-groups/{id}/members", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(UUID.randomUUID()))
                    )
                    .andExpect(status().is(422));
        }
    }

    @Nested
    @DisplayName("POST /api/training-groups/{id}/trainers")
    class AddTrainerTests {

        @Test
        @DisplayName("should return 204 when adding a trainer")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldReturn204WhenAddingTrainer() throws Exception {
            mockMvc.perform(
                            post("/api/training-groups/{id}/trainers", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(UUID.randomUUID()))
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when user lacks GROUPS:TRAINING authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingTrainingAuthority() throws Exception {
            mockMvc.perform(
                            post("/api/training-groups/{id}/trainers", GROUP_UUID)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {"memberId": "%s"}
                                            """.formatted(UUID.randomUUID()))
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/training-groups/{id}/trainers/{memberId}")
    class RemoveTrainerTests {

        @Test
        @DisplayName("should return 422 when removing last trainer")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldReturn422WhenRemovingLastTrainer() throws Exception {
            doThrow(new CannotRemoveLastOwnerException(new UserId(UUID.fromString(TRAINER_ID))))
                    .when(trainingGroupManagementService).removeTrainer(any(TrainingGroupId.class), any(MemberId.class));

            mockMvc.perform(
                            delete("/api/training-groups/{id}/trainers/{memberId}", GROUP_UUID, UUID.fromString(TRAINER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().is(422));
        }

        @Test
        @DisplayName("should return 204 when removing a trainer successfully")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldReturn204WhenRemovingTrainer() throws Exception {
            mockMvc.perform(
                            delete("/api/training-groups/{id}/trainers/{memberId}", GROUP_UUID, UUID.fromString(TRAINER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when user lacks GROUPS:TRAINING authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingTrainingAuthority() throws Exception {
            mockMvc.perform(
                            delete("/api/training-groups/{id}/trainers/{memberId}", GROUP_UUID, UUID.fromString(TRAINER_ID))
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/training-groups/{id}")
    class DeleteTrainingGroupTests {

        @Test
        @DisplayName("should return 204 when user has GROUPS:TRAINING authority")
        @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.GROUPS_TRAINING})
        void shouldReturn204WhenAuthorized() throws Exception {
            mockMvc.perform(
                            delete("/api/training-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 403 when user lacks GROUPS:TRAINING authority")
        @WithKlabisMockUser(memberId = MEMBER_ID)
        void shouldReturn403WhenMissingTrainingAuthority() throws Exception {
            mockMvc.perform(
                            delete("/api/training-groups/{id}", GROUP_UUID)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }
    }
}
