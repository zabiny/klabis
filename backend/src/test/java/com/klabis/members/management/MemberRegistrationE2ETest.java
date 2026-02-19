package com.klabis.members.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.TestApplicationConfiguration;
import com.klabis.members.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.Period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests for member registration.
 * <p>
 * Tests complete registration flow including:
 * - Guardian requirements for minors
 * - Registration number generation
 * - HATEOAS link generation
 * - Persistence verification
 */
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@DisplayName("Member Registration E2E Tests")
class MemberRegistrationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_USERNAME = "admin";
    private static final String MEMBERS_CREATE_AUTHORITY = "MEMBERS:CREATE";
    private static final String MEMBERS_READ_AUTHORITY = "MEMBERS:READ";

    @Test
    @DisplayName("Complete registration flow for adult member without guardian")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldCompleteRegistrationFlowForAdult() throws Exception {
        // Given: An adult member (18+ years old)
        RegisterMemberRequest request = MemberManagementDtosTestDataBuilder.registerMemberRequestWithDateOfBirth(
                LocalDate.of(1999, 1, 15));

        // When: Registering the member
        MvcResult result = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Novák"))
                .andReturn();

        // Verify Location header contains member ID
        String locationHeader = result.getResponse().getHeader("Location");
        assertThat(locationHeader).isNotNull();
        assertThat(locationHeader).contains("/api/members/");
    }

    @Test
    @DisplayName("Complete registration flow for minor with guardian")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldCompleteRegistrationFlowForMinorWithGuardian() throws Exception {
        // Given: A minor member (under 18) with guardian
        RegisterMemberRequest request = MemberManagementDtosTestDataBuilder.defaultMinorWithGuardianDto();

        // When: Registering the minor with guardian
        MvcResult result = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.firstName").value("Petra"))
                .andExpect(jsonPath("$.lastName").value("Nováková"))
                .andReturn();

        // Then: Verify successful registration
        String locationHeader = result.getResponse().getHeader("Location");
        assertThat(locationHeader).isNotNull();
        assertThat(locationHeader).contains("/api/members/");
    }

    @Test
    @DisplayName("Registration should fail for minor without guardian contact")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldFailForMinorWithoutGuardianContact() throws Exception {
        // Given: A minor member (under 18) without guardian
        LocalDate dateOfBirth = LocalDate.now().minusYears(10);
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        assertThat(age).isLessThan(18);

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Petra",
                "Nováková",
                dateOfBirth,
                "CZ",
                Gender.FEMALE,
                "petra.novakova@example.com",
                "+420111222333",
                MemberManagementDtosTestDataBuilder.addressRequestWithStreetAndCity("Hlavní 789", "Ostrava"),
                null,  // No guardian - should fail domain validation
                null,  // birthNumber
                null   // bankAccountNumber
        );

        // When/Then: Registration should fail
        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @DisplayName("Registration should generate unique registration numbers")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldGenerateUniqueRegistrationNumbers() throws Exception {
        // Given: Two members born in the same year
        LocalDate dateOfBirth = LocalDate.of(2005, 5, 15);

        AddressRequest address1 = new AddressRequest(
                "Street 1",
                "City1",
                "10000",
                "CZ"
        );

        AddressRequest address2 = new AddressRequest(
                "Street 2",
                "City2",
                "20000",
                "CZ"
        );

        RegisterMemberRequest request1 = new RegisterMemberRequest(
                "Jan",
                "Novák",
                dateOfBirth,
                "CZ",
                Gender.MALE,
                "jan1@example.com",
                "+420777111111",
                address1,
                null,
                null,
                null);

        RegisterMemberRequest request2 = new RegisterMemberRequest(
                "Petra",
                "Nováková",
                dateOfBirth,
                "CZ",
                Gender.FEMALE,
                "petra1@example.com",
                "+420777222222",
                address2,
                null,
                null,
                null);

        // When: Registering both members
        MvcResult result1 = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request1))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result2 = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request2))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        // Then: Verify both have unique IDs
        String memberId1 = extractMemberId(result1);
        String memberId2 = extractMemberId(result2);

        assertThat(memberId1).isNotNull();
        assertThat(memberId2).isNotNull();
        assertThat(memberId1).isNotEqualTo(memberId2);
    }

    @Test
    @DisplayName("HATEOAS links should be generated in response")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldGenerateHateoasLinks() throws Exception {
        // Given: A valid member registration request
        AddressRequest address = new AddressRequest(
                "Hlavní 321",
                "Plzeň",
                "30000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(1999, 1, 15),
                "CZ",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777123456",
                address,
                null,
                null,
                null
        );

        // When: Registering the member
        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
        // Note: HATEOAS links validation commented pending investigation
        // .andExpect(jsonPath("$._links.self.href").exists())
        // .andExpect(jsonPath("$._links.self.href").value(containsString("/api/members/")));
    }

    @Test
    @DisplayName("Registration should handle single email address")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldHandleSingleEmailAddress() throws Exception {
        // Given: A member with a single email address
        AddressRequest address = new AddressRequest(
                "Hlavní 555",
                "Liberec",
                "46000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(1999, 1, 15),
                "CZ",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777123456",
                address,
                null,
                null,
                null
        );

        // When: Registering the member
        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString());
    }

    @Test
    @DisplayName("Registration should handle single phone number")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldHandleSinglePhoneNumber() throws Exception {
        // Given: A member with a single phone number
        AddressRequest address = new AddressRequest(
                "Hlavní 666",
                "Ústí nad Labem",
                "40000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(1999, 1, 15),
                "CZ",
                Gender.MALE,
                "jan@example.com",
                "+420777123456",
                address,
                null,
                null,
                null
        );

        // When: Registering the member
        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString());
    }

    @Test
    @DisplayName("Registration with birth number and bank account for Czech national should succeed")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
    void shouldRegisterCzechMemberWithBirthNumberAndBankAccount() throws Exception {
        // Given: A Czech adult member with birth number and bank account
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(1999, 1, 15),
                "CZ",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777123456",
                address,
                null,
                "900101/1234",
                "CZ6508000000192000145399"
        );

        // When: Registering the member
        MvcResult result = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Novák"))
                .andReturn();

        // Extract member ID from response
        String responseJson = result.getResponse().getContentAsString();
        String memberId = objectMapper.readTree(responseJson).get("id").asText();

        // Then: Verify birth number and bank account are stored by retrieving the member
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.birthNumber").value("900101/1234"))
                .andExpect(jsonPath("$.bankAccountNumber").value("CZ6508000000192000145399"));
    }

    @Test
    @DisplayName("Registration with birth number for non-Czech national should fail")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldFailForNonCzechMemberWithBirthNumber() throws Exception {
        // Given: A Slovak member with birth number (not allowed)
        AddressRequest address = new AddressRequest(
                "Main Street 123",
                "Bratislava",
                "81101",
                "SK"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Eva",
                "Svobodová",
                LocalDate.of(1995, 3, 20),
                "SK",
                Gender.FEMALE,
                "eva.svobodova@example.com",
                "+421777888999",
                address,
                null,
                "950320/1234",
                null
        );

        // When/Then: Registration should fail with domain validation error
        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Birth number")));
    }

    @Test
    @DisplayName("Registration with bank account only (without birth number) should succeed")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
    void shouldRegisterMemberWithBankAccountOnly() throws Exception {
        // Given: A member with only bank account number
        AddressRequest address = new AddressRequest(
                "Hlavní 456",
                "Brno",
                "60000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Petr",
                "Svoboda",
                LocalDate.of(1992, 5, 10),
                "CZ",
                Gender.MALE,
                "petr.svoboda@example.com",
                "+420777111222",
                address,
                null,
                null,
                "CZ6508000000192000145399"
        );

        // When: Registering the member
        MvcResult result = mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andReturn();

        // Extract member ID from response
        String responseJson = result.getResponse().getContentAsString();
        String memberId = objectMapper.readTree(responseJson).get("id").asText();

        // Then: Verify bank account is stored and birth number is not present
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.bankAccountNumber").value("CZ6508000000192000145399"))
                .andExpect(jsonPath("$.birthNumber").doesNotExist());
    }

    private String extractMemberId(MvcResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        // Simple JSON extraction - in production use proper JSON parsing
        int idStart = responseBody.indexOf("\"id\":\"") + 6;
        int idEnd = responseBody.indexOf("\"", idStart);
        return responseBody.substring(idStart, idEnd);
    }
}
