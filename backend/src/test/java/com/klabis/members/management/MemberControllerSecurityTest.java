package com.klabis.members.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.SecurityTestBase;
import com.klabis.members.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Member Controller Security Tests")
@ApplicationModuleTest(extraIncludes = {"users", "common"}, mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
// need users for Security configuration, common for EmailService
@Import(TestApplicationConfiguration.class)
class MemberControllerSecurityTest extends SecurityTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    private RegisterMemberRequest createValidMemberRequest() {
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        return new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(2005, 5, 15),
                "CZ",
                Gender.MALE,
                "jan@example.com",
                "+420777123456",
                address,
                null
        );
    }

    @Test
    @DisplayName("POST /api/members without authentication should return 401")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        RegisterMemberRequest request = createValidMemberRequest();

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/members with wrong authority should return 403")
    @WithMockUser(username = "ZBM0102", authorities = {"MEMBERS:READ"})
    void shouldReturn403WhenInsufficientAuthority() throws Exception {
        RegisterMemberRequest request = createValidMemberRequest();

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("POST /api/members with MEMBERS:CREATE authority should return 201")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldReturn201WhenAuthorized() throws Exception {
        RegisterMemberRequest request = createValidMemberRequest();

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Novák"));
        // TODO: HATEOAS links disabled pending investigation of serialization
        // .andExpect(jsonPath("$._links.self.href").exists());
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
    @WithMockUser(username = "ZBM0102", authorities = {"SOME:OTHER"})
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
    @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
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
    @WithMockUser(username = "ZBM0102", authorities = {"SOME:OTHER"})
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
    @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
    void shouldReturn200WhenListingMembersWithReadAuthority() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .contentType("application/json")
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }
}
