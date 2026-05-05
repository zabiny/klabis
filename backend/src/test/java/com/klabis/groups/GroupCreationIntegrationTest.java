package com.klabis.groups;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestApplicationConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@CleanupTestData
@DisplayName("Group creation integration — all 3 group types via REST API")
public class GroupCreationIntegrationTest {

    private static final String MEMBER_UUID = "11111111-1111-1111-1111-111111111111";
    private static final String ADMIN_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String TRAINER_UUID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithKlabisMockUser(memberId = MEMBER_UUID)
    @DisplayName("1.2 authenticated regular user creates FreeGroup via POST /api/groups — expect 201")
    void shouldCreateFreeGroupAndReturn201() throws Exception {
        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test free group"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_MANAGE})
    @DisplayName("1.3 admin user creates FamilyGroup via POST /api/family-groups — expect 201")
    void shouldCreateFamilyGroupAndReturn201() throws Exception {
        mockMvc.perform(post("/api/family-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test family", "parent": "%s"}
                                """.formatted(ADMIN_UUID)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithKlabisMockUser(memberId = TRAINER_UUID, authorities = {Authority.GROUPS_TRAINING})
    @DisplayName("1.4 user with GROUPS:TRAINING creates TrainingGroup via POST /api/training-groups — expect 201")
    void shouldCreateTrainingGroupAndReturn201() throws Exception {
        mockMvc.perform(post("/api/training-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test training", "trainerId": "%s", "minAge": 10, "maxAge": 14}
                                """.formatted(TRAINER_UUID)))
                .andExpect(status().isCreated());
    }
}
