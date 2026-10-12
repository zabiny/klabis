package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.common.HalShape;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.users.Authority;
import com.klabis.groups.GroupsWebMvcTest;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.application.TrainingGroupManagementPort;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Training group detail HAL parity (trainers, members)")
@GroupsWebMvcTest
class TrainingGroupDetailHalParityTest {

    private static final String TRAINER_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final String SECOND_TRAINER_ID = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee";
    private static final String MEMBER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final String GROUP = "/api/training-groups/" + GROUP_UUID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TrainingGroupManagementPort trainingGroupManagement;

    private static MemberId member(String id) {
        return new MemberId(UUID.fromString(id));
    }

    private String getGroupBody(Set<MemberId> trainers) throws Exception {
        TrainingGroup group = TrainingGroup.reconstruct(new TrainingGroupId(GROUP_UUID), "Juniors", trainers,
                Set.of(GroupMembership.of(member(MEMBER_ID))), new AgeRange(10, 18), null);
        when(trainingGroupManagement.getTrainingGroup(any(TrainingGroupId.class))).thenReturn(group);
        return mockMvc.perform(get("/api/training-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static List<String> item(String body, String property, String memberId) {
        int index = com.jayway.jsonpath.JsonPath.<List<String>>read(body, "$." + property + "[*].memberId")
                .indexOf(memberId);
        return HalShape.of(body, "$." + property + "[" + index + "]");
    }

    @Test
    @DisplayName("training manager sees removal affordances on trainers (when more than one) and members")
    @WithKlabisMockUser(memberId = TRAINER_ID, authorities = {Authority.MEMBERS_READ, Authority.GROUPS_TRAINING})
    void itemsForTrainingManager() throws Exception {
        String body = getGroupBody(Set.of(member(TRAINER_ID), member(SECOND_TRAINER_ID)));

        assertThat(item(body, "trainers", SECOND_TRAINER_ID)).isEqualTo(List.of("link member -> /api/members/" + SECOND_TRAINER_ID, "link self -> " + GROUP + "/trainers/" + SECOND_TRAINER_ID, "template removeTrainer DELETE []"));
        assertThat(item(body, "members", MEMBER_ID)).isEqualTo(List.of("link member -> /api/members/" + MEMBER_ID, "link self -> " + GROUP + "/members/" + MEMBER_ID, "template removeTrainingGroupMember DELETE []"));
    }

    @Test
    @DisplayName("training manager sees no removal affordance on the only trainer")
    @WithKlabisMockUser(memberId = TRAINER_ID, authorities = {Authority.MEMBERS_READ, Authority.GROUPS_TRAINING})
    void singleTrainerHasNoRemoval() throws Exception {
        String body = getGroupBody(Set.of(member(TRAINER_ID)));

        assertThat(item(body, "trainers", TRAINER_ID)).isEqualTo(List.of("link member -> /api/members/" + TRAINER_ID));
    }

    @Test
    @DisplayName("training manager without MEMBERS:READ loses only the member links")
    @WithKlabisMockUser(memberId = TRAINER_ID, authorities = {Authority.GROUPS_TRAINING})
    void managerWithoutMembersRead() throws Exception {
        String body = getGroupBody(Set.of(member(TRAINER_ID), member(SECOND_TRAINER_ID)));

        assertThat(item(body, "trainers", SECOND_TRAINER_ID)).isEqualTo(List.of("link self -> " + GROUP + "/trainers/" + SECOND_TRAINER_ID, "template removeTrainer DELETE []"));
        assertThat(item(body, "members", MEMBER_ID)).isEqualTo(List.of("link self -> " + GROUP + "/members/" + MEMBER_ID, "template removeTrainingGroupMember DELETE []"));
    }

    @Test
    @DisplayName("plain group member gets the limited response: trainers with member links only, no members")
    @WithKlabisMockUser(memberId = MEMBER_ID, authorities = {Authority.MEMBERS_READ})
    void limitedResponseForGroupMember() throws Exception {
        String body = getGroupBody(Set.of(member(TRAINER_ID), member(SECOND_TRAINER_ID)));

        assertThat(item(body, "trainers", SECOND_TRAINER_ID)).isEqualTo(List.of("link member -> /api/members/" + SECOND_TRAINER_ID));
        assertThat(com.jayway.jsonpath.JsonPath.<java.util.Map<String, Object>>read(body, "$"))
                .doesNotContainKeys("members", "ageRange");
    }
}
