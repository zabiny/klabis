package com.klabis.members;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for PATCH /api/members/{id} endpoint.
 * <p>
 * Tests the complete update flow through the application layer with real database.
 * Uses {@link ApplicationModuleTest} to test within the members module boundary.
 * <p>
 * <b>Test Scope:</b>
 * <ul>
 *   <li>Admin update flow with MEMBERS_MANAGE authority</li>
 *   <li>Member self-update flow (own data only)</li>
 *   <li>Data persistence verification through GET endpoint</li>
 * </ul>
 * <p>
 * <b>What is NOT tested here:</b>
 * <ul>
 *   <li>Error responses (400, 403, 404, 401) - tested in controller unit tests (UpdateMemberApiTest)</li>
 *   <li>Detailed JSON field-by-field validation - tested in controller unit tests</li>
 *   <li>Domain update logic - tested in domain unit tests</li>
 *   <li>E2E lifecycle flows - tested in {@link MemberLifecycleE2ETest}</li>
 * </ul>
 */
@ApplicationModuleTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PATCH /api/members/{id} Integration Tests")
class UpdateMemberIntegrationTest {

    private static final UUID TEST_MEMBER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithKlabisMockUser(username = "admin", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    @Sql(scripts = "/sql/test-members-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("admin: should update member email, phone, address and verify data persistence")
    void shouldUpdateMemberAsAdminAndPersist() throws Exception {
        mockMvc.perform(
                        patch("/api/members/{id}", TEST_MEMBER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "email": "admin-updated@example.com",
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

        // Verify data persisted through GET endpoint
        mockMvc.perform(get("/api/members/{id}", TEST_MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))   // verify other attributes are untouched
                .andExpect(jsonPath("$.lastName").value("User"))   // verify other attributes are untouched
                .andExpect(jsonPath("$.email").value("admin-updated@example.com"))
                .andExpect(jsonPath("$.phone").value("+420999888777"))
                .andExpect(jsonPath("$.address.street").value("Nová 123"))
                .andExpect(jsonPath("$.address.city").value("Brno"));
    }

    @Test
    @WithKlabisMockUser(username = "admin", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    @Sql(scripts = "/sql/test-members-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("admin: should update admin-only fields (firstName, lastName)")
    void shouldUpdateAdminOnlyFields() throws Exception {
        mockMvc.perform(
                        patch("/api/members/{id}", TEST_MEMBER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "firstName": "JanUpdated",
                                            "lastName": "NovákUpdated"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/members/{id}", TEST_MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test.user@example.com"))  // verify other attribute is unchanged
                .andExpect(jsonPath("$.firstName").value("JanUpdated"))
                .andExpect(jsonPath("$.lastName").value("NovákUpdated"));
    }

    @Test
    @WithKlabisMockUser(userId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ})
    @Sql(scripts = "/sql/test-members-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("member: should update own email, phone, dietaryRestrictions and verify data persistence")
    void shouldUpdateOwnDataAsMemberAndPersist() throws Exception {
        mockMvc.perform(
                        patch("/api/members/{id}", TEST_MEMBER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "email": "self-updated@example.com",
                                            "phone": "+420111222333",
                                            "dietaryRestrictions": "Vegetarian, no nuts"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        // Verify data persisted through GET endpoint
        mockMvc.perform(get("/api/members/{id}", TEST_MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Test"))   // verify other attributes are untouched
                .andExpect(jsonPath("$.lastName").value("User"))   // verify other attributes are untouched
                .andExpect(jsonPath("$.email").value("self-updated@example.com"))
                .andExpect(jsonPath("$.phone").value("+420111222333"))
                .andExpect(jsonPath("$.dietaryRestrictions").value("Vegetarian, no nuts"));
    }

    @Test
    @WithKlabisMockUser(userId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ})
    @Sql(scripts = "/sql/test-members-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("member: self-update should apply all non-admin fields including chipNumber and nationality")
    void selfUpdateShouldApplyNonAdminFields() throws Exception {
        mockMvc.perform(
                        patch("/api/members/{id}", TEST_MEMBER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "email": "self-updated@example.com",
                                            "chipNumber": "123456",
                                            "nationality": "SK"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/members/{id}", TEST_MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("self-updated@example.com"))
                .andExpect(jsonPath("$.chipNumber").value("123456"))
                .andExpect(jsonPath("$.nationality").value("SK"));
    }

    @Test
    @WithKlabisMockUser(userId = "11111111-1111-1111-1111-111111111111", authorities = {Authority.MEMBERS_READ})
    @Sql(scripts = "/sql/test-members-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("member: admin-only fields (firstName, lastName, gender) should be silently ignored in self-update")
    void adminOnlyFieldsShouldBeIgnoredInSelfUpdate() throws Exception {
        mockMvc.perform(
                        patch("/api/members/{id}", TEST_MEMBER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "email": "self-updated@example.com",
                                            "firstName": "Hacked",
                                            "lastName": "Name",
                                            "gender": "FEMALE"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/members/{id}", TEST_MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("self-updated@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.gender").value("MALE"));
    }

    @Test
    @WithKlabisMockUser(username = "admin", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    @Sql(scripts = "/sql/test-members-setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @DisplayName("admin: should update gender and birthNumber for Czech member")
    void shouldUpdateGenderAndBirthNumberForCzechMember() throws Exception {
        mockMvc.perform(
                        patch("/api/members/{id}", TEST_MEMBER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "gender": "FEMALE",
                                            "birthNumber": "950215/2345"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        // Verify data persisted
        mockMvc.perform(get("/api/members/{id}", TEST_MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gender").value("FEMALE"));
    }
}
