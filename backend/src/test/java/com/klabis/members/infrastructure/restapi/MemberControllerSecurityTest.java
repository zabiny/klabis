package com.klabis.members.infrastructure.restapi;

import com.klabis.TestApplicationConfiguration;
import com.klabis.common.SecurityTestBase;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.members.LastOwnershipChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Member Controller Security Tests")
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
// need users for Security configuration, common for EmailService
@Import(TestApplicationConfiguration.class)
class MemberControllerSecurityTest extends SecurityTestBase {

    @MockitoBean
    @SuppressWarnings("unused")
    private LastOwnershipChecker lastOwnershipChecker;

    @Test
    @DisplayName("POST /api/members without authentication should return 401")
    void shouldReturn401WhenUnauthenticated() throws Exception {

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "firstName": "Jan",
                                            "lastName": "Novák",
                                            "dateOfBirth": "2005-05-15",
                                            "nationality": "CZ",
                                            "gender": "MALE",
                                            "email": "jan@example.com",
                                            "phone": "+420777123456",
                                            "address": {
                                                "street": "Hlavní 123",
                                                "city": "Praha",
                                                "postalCode": "11000",
                                                "country": "CZ"
                                            }
                                        }
                                        """)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/members with wrong authority should return 403")
    @WithKlabisMockUser(username = "ZBM0102", authorities = {Authority.MEMBERS_READ})
    void shouldReturn403WhenInsufficientAuthority() throws Exception {

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "firstName": "Jan",
                                            "lastName": "Novák",
                                            "dateOfBirth": "2005-05-15",
                                            "nationality": "CZ",
                                            "gender": "MALE",
                                            "email": "jan@example.com",
                                            "phone": "+420777123456",
                                            "address": {
                                                "street": "Hlavní 123",
                                                "city": "Praha",
                                                "postalCode": "11000",
                                                "country": "CZ"
                                            }
                                        }
                                        """)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/members with MEMBERS:MANAGE authority should return 201")
    @WithKlabisMockUser(authorities =  {Authority.MEMBERS_MANAGE})
    void shouldReturn201WhenAuthorized() throws Exception {

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "firstName": "Jan",
                                            "lastName": "Novák",
                                            "dateOfBirth": "2005-05-15",
                                            "nationality": "CZ",
                                            "gender": "MALE",
                                            "email": "jan@example.com",
                                            "phone": "+420777123456",
                                            "birthNumber": "050515/1234",
                                            "address": {
                                                "street": "Hlavní 123",
                                                "city": "Praha",
                                                "postalCode": "11000",
                                                "country": "CZ"
                                            }
                                        }
                                        """)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION));
    }

    @Test
    @DisplayName("GET /api/members/{id} without authentication should return 401")
    void shouldReturn401WhenGettingMemberUnauthenticated() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/members/" + memberId)
                                .contentType("application/json")
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/members/{id} with wrong authority should return 403")
    @WithKlabisMockUser(username = "ZBM0102", authorities = {})
    void shouldReturn403WhenGettingMemberWithoutReadAuthority() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(
                        get("/api/members/" + memberId)
                                .contentType("application/json")
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    @DisplayName("GET /api/members/{id} with MEMBERS:READ authority should pass authorization")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldPassAuthorizationWhenGettingMemberWithReadAuthority() throws Exception {
        UUID memberId = UUID.randomUUID();

        // This test validates that authorization passes - the endpoint returns
        // 404 Not Found for non-existent member IDs (which is expected behavior)
        // If authorization was denied, we'd get 403 before reaching the endpoint
        mockMvc.perform(
                        get("/api/members/" + memberId)
                                .contentType("application/json")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("GET /api/members without authentication should return 401")
    void shouldReturn401WhenListingMembersUnauthenticated() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .contentType("application/json")
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/members with wrong authority should return 403")
    @WithKlabisMockUser(username = "ZBM0102", authorities = {Authority.CALENDAR_MANAGE})
    void shouldReturn403WhenListingMembersWithoutReadAuthority() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .contentType("application/json")
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /api/members with MEMBERS:READ authority should return 200")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldReturn200WhenListingMembersWithReadAuthority() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .contentType("application/json")
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/members/{id}/suspend without authentication should return 401")
    void shouldReturn401WhenSuspendingMemberUnauthenticated() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/members/" + memberId + "/suspend")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "reason": "ODHLASKA",
                                            "note": "Test termination"
                                        }
                                        """)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/members/{id}/suspend with wrong authority should return 403")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldReturn403WhenSuspendingMemberWithoutUpdateAuthority() throws Exception {
        UUID memberId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/members/" + memberId + "/suspend")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "reason": "ODHLASKA",
                                            "note": "Test termination"
                                        }
                                        """)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/members/{id}/suspend with MEMBERS:MANAGE authority should pass authorization")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldPassAuthorizationWhenSuspendingMemberWithUpdateAuthority() throws Exception {
        UUID memberId = UUID.randomUUID();

        // This test validates that authorization passes (not denied with 403).
        // A non-existent member returns 404, which confirms authorization succeeded.
        mockMvc.perform(
                        post("/api/members/" + memberId + "/suspend")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "reason": "ODHLASKA",
                                            "note": "Test termination"
                                        }
                                        """)
                )
                .andExpect(status().isNotFound());
    }
}
