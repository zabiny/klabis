package com.klabis.members;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ApplicationModuleTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UpdateMember persistence smoke test")
class UpdateMemberPersistenceTest {

    private static final UUID TEST_MEMBER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @MockitoBean
    @SuppressWarnings("unused")
    private LastOwnershipChecker lastOwnershipChecker;

    @MockitoBean
    @SuppressWarnings("unused")
    private com.klabis.members.TrainingGroupProvider trainingGroupProvider;

    @MockitoBean
    @SuppressWarnings("unused")
    private com.klabis.members.FamilyGroupProvider familyGroupProvider;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithKlabisMockUser(username = "admin", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    @Sql(scripts = "/sql/test-members-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("admin PATCH email, phone and address should persist through the full stack")
    void adminUpdateShouldPersistThroughFullStack() throws Exception {
        mockMvc.perform(
                        patch("/api/members/{id}", TEST_MEMBER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "email": "updated@example.com",
                                            "phone": "+420999888777",
                                            "address": {
                                                "street": "Nová 123",
                                                "city": "Brno",
                                                "postalCode": "60000",
                                                "country": "CZ"
                                            }
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/members/{id}", TEST_MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.phone").value("+420999888777"))
                .andExpect(jsonPath("$.address.street").value("Nová 123"))
                .andExpect(jsonPath("$.address.city").value("Brno"));
    }
}
