package com.klabis.members.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.E2EIntegrationTest;
import com.klabis.members.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests for GET /api/members/{id} endpoint.
 * <p>
 * Tests complete member retrieval flow including:
 * - Member creation and subsequent retrieval
 * - Guardian information handling
 * - Full member details verification
 * - Non-existent member ID handling
 */
// TODO: refactor into WebMvcTest (minimalize E2E tests.. only for more complex things)
@E2EIntegrationTest
@ActiveProfiles("test")
@DisplayName("Get Member E2E Tests")
class GetMemberE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_USERNAME = "admin";
    private static final String MEMBERS_CREATE_AUTHORITY = "MEMBERS:CREATE";
    private static final String MEMBERS_READ_AUTHORITY = "MEMBERS:READ";

    @Test
    @DisplayName("Complete flow: create member and retrieve by ID")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
    void shouldCreateMemberAndRetrieveById() throws Exception {
        // Given: Create a member
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest createRequest = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(2005, 6, 15),
                "CZ",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777888999",
                address,
                null
        );

        MvcResult createResult = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createRequest))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Novák"))
                .andReturn();

        // Extract member ID from response
        String responseJson = createResult.getResponse().getContentAsString();
        String memberId = objectMapper.readTree(responseJson).get("id").asText();

        // When: Retrieve the member by ID
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Novák"))
                .andExpect(jsonPath("$.dateOfBirth").value("2005-06-15"))
                .andExpect(jsonPath("$.nationality").value("CZ"))
                .andExpect(jsonPath("$.gender").value("MALE"))
                .andExpect(jsonPath("$.email").value("jan.novak@example.com"))
                .andExpect(jsonPath("$.phone").value("+420777888999"))
                .andExpect(jsonPath("$.address.street").value("Hlavní 123"))
                .andExpect(jsonPath("$.address.city").value("Praha"))
                .andExpect(jsonPath("$.address.postalCode").value("11000"))
                .andExpect(jsonPath("$.address.country").value("CZ"))
                .andExpect(jsonPath("$.active").value(true));
        // Note: guardian field is null and not included in JSON due to @JsonInclude(NON_NULL)
    }

    @Test
    @DisplayName("Complete flow: create member with guardian and retrieve by ID")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
    void shouldCreateMemberWithGuardianAndRetrieveById() throws Exception {
        // Given: Create a minor member with guardian
        LocalDate dateOfBirth = LocalDate.now().minusYears(10);

        GuardianDTO guardian = new GuardianDTO(
                "Pavel",
                "Novák",
                "PARENT",
                "pavel.novak@example.com",
                "+420987654321"
        );

        AddressRequest address = new AddressRequest(
                "Hlavní 456",
                "Brno",
                "60000",
                "CZ"
        );

        RegisterMemberRequest createRequest = new RegisterMemberRequest(
                "Petra",
                "Nováková",
                dateOfBirth,
                "CZ",
                Gender.FEMALE,
                "petra.novakova@example.com",
                "+420111222333",
                address,
                guardian
        );

        MvcResult createResult = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createRequest))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        // Extract member ID from response
        String responseJson = createResult.getResponse().getContentAsString();
        String memberId = objectMapper.readTree(responseJson).get("id").asText();

        // When: Retrieve the member by ID
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.firstName").value("Petra"))
                .andExpect(jsonPath("$.lastName").value("Nováková"))
                .andExpect(jsonPath("$.email").value("petra.novakova@example.com"))
                .andExpect(jsonPath("$.phone").value("+420111222333"))
                .andExpect(jsonPath("$.address.street").value("Hlavní 456"))
                .andExpect(jsonPath("$.address.city").value("Brno"))
                .andExpect(jsonPath("$.guardian").isNotEmpty())
                .andExpect(jsonPath("$.guardian.firstName").value("Pavel"))
                .andExpect(jsonPath("$.guardian.lastName").value("Novák"))
                .andExpect(jsonPath("$.guardian.relationship").value("PARENT"))
                .andExpect(jsonPath("$.guardian.email").value("pavel.novak@example.com"))
                .andExpect(jsonPath("$.guardian.phone").value("+420987654321"));
    }

    @Test
    @DisplayName("GET /api/members/{id} should return 404 for non-existent member ID")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
    void shouldReturn404ForNonExistentMemberId() throws Exception {
        // Given: A non-existent UUID
        String nonExistentId = UUID.randomUUID().toString();

        // When & Then: Retrieve should return 404
        mockMvc.perform(
                        get("/api/members/{id}", nonExistentId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(nonExistentId)));
    }

    @Test
    @DisplayName("Complete flow: create adult member and verify all details")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
    void shouldCreateAdultMemberAndVerifyAllDetails() throws Exception {
        // Given: Create an adult member
        AddressRequest address = new AddressRequest(
                "Main Street 123",
                "Bratislava",
                "81101",
                "SK"
        );

        RegisterMemberRequest createRequest = new RegisterMemberRequest(
                "Eva",
                "Svobodová",
                LocalDate.of(1995, 3, 20),
                "SK",
                Gender.FEMALE,
                "eva.svobodova@example.com",
                "+421777888999",
                address,
                null
        );

        MvcResult createResult = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createRequest))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        // Extract member ID from response
        String responseJson = createResult.getResponse().getContentAsString();
        String memberId = objectMapper.readTree(responseJson).get("id").asText();

        // When: Retrieve the member by ID
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.firstName").value("Eva"))
                .andExpect(jsonPath("$.lastName").value("Svobodová"))
                .andExpect(jsonPath("$.nationality").value("SK"))
                .andExpect(jsonPath("$.gender").value("FEMALE"))
                .andExpect(jsonPath("$.dateOfBirth").value("1995-03-20"))
                .andExpect(jsonPath("$.email").value("eva.svobodova@example.com"))
                .andExpect(jsonPath("$.phone").value("+421777888999"))
                .andExpect(jsonPath("$.address.street").value("Main Street 123"))
                .andExpect(jsonPath("$.address.city").value("Bratislava"))
                .andExpect(jsonPath("$.address.postalCode").value("81101"))
                .andExpect(jsonPath("$.address.country").value("SK"))
                .andExpect(jsonPath("$.active").value(true));
        // Note: guardian field is null and not included in JSON due to @JsonInclude(NON_NULL)
    }
}
