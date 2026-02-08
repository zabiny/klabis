package com.klabis.members.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.SecurityTestBase;
import com.klabis.members.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for MemberController.
 * <p>
 * Tests validation, error handling, and HATEOAS link generation.
 */
@DisplayName("Member Controller API Tests")
@ApplicationModuleTest(extraIncludes = {"users", "common"}, mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
// need users for Security configuration, common for EmailService
@Import(TestApplicationConfiguration.class)
class MemberControllerApiTest extends SecurityTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/members with valid data should return 201 with HATEOAS links")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldCreateMemberWithValidData() throws Exception {
        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                MemberManagementDtosTestDataBuilder.DEFAULT_ADULT_DATE_OF_BIRTH,
                "CZ",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777123456",
                MemberManagementDtosTestDataBuilder.defaultAddressRequest(),
                null
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Novák"))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("POST /api/members with minor should accept guardian")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldCreateMinorWithGuardian() throws Exception {
        RegisterMemberRequest request = new RegisterMemberRequest(
                "Petra",
                "Nováková",
                LocalDate.of(2010, 6, 20),
                "CZ",
                Gender.FEMALE,
                "petra.novakova@example.com",
                "+420111222333",
                MemberManagementDtosTestDataBuilder.addressRequestWithStreetAndCity("Hlavní 456", "Brno"),
                MemberManagementDtosTestDataBuilder.defaultGuardianDto()
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString());
    }

    @Test
    @DisplayName("POST /api/members with missing first name should return 400")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldReturn400WhenFirstNameMissing() throws Exception {
        RegisterMemberRequest request = new RegisterMemberRequest(
                "",  // Invalid: empty first name
                "Novák",
                MemberManagementDtosTestDataBuilder.DEFAULT_ADULT_DATE_OF_BIRTH,
                "CZ",
                Gender.MALE,
                "jan@example.com",
                "+420777123456",
                MemberManagementDtosTestDataBuilder.defaultAddressRequest(),
                null
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://klabis.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors.firstName").exists());
    }

    @Test
    @DisplayName("POST /api/members with invalid email should return 400")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldReturn400WhenEmailInvalid() throws Exception {
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(2005, 5, 15),
                "CZ",
                Gender.MALE,
                "invalid-email",  // Invalid email format
                "+420777123456",
                address,
                null
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://klabis.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("POST /api/members with invalid phone should return 400")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldReturn400WhenPhoneInvalid() throws Exception {
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(2005, 5, 15),
                "CZ",
                Gender.MALE,
                "jan@example.com",
                "123",  // Invalid phone format
                address,
                null
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://klabis.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    @DisplayName("POST /api/members with future date of birth should return 400")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldReturn400WhenDateOfBirthInFuture() throws Exception {
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.now().plusDays(1),  // Future date
                "CZ",
                Gender.MALE,
                "jan@example.com",
                "+420777123456",
                address,
                null
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://klabis.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors.dateOfBirth").exists());
    }

    @Test
    @DisplayName("POST /api/members with missing email should return 400")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldReturn400WhenEmailMissing() throws Exception {
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(2005, 5, 15),
                "CZ",
                Gender.MALE,
                "",  // Invalid: empty email
                "+420777123456",
                address,
                null
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://klabis.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    @DisplayName("POST /api/members with invalid guardian should return 400")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldReturn400WhenGuardianInvalid() throws Exception {
        GuardianDTO guardian = new GuardianDTO(
                "",  // Invalid: empty first name
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

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Petra",
                "Nováková",
                LocalDate.of(2010, 6, 20),
                "CZ",
                Gender.FEMALE,
                "petra@example.com",
                "+420111222333",
                address,
                guardian
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://klabis.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors['guardian.firstName']").exists());
    }

    @Test
    @DisplayName("POST /api/members with valid address and contacts should succeed")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldCreateMemberWithValidAddressAndContacts() throws Exception {
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(2005, 5, 15),
                "CZ",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777123456",
                address,
                null
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString());
    }

    @Test
    @DisplayName("POST /api/members with invalid nationality code should return 400")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldReturn400WhenNationalityCodeInvalid() throws Exception {
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(2005, 5, 15),
                "CZECH",  // Invalid: too long (should be 2-3 chars)
                Gender.MALE,
                "jan@example.com",
                "+420777123456",
                address,
                null
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://klabis.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors.nationality").exists());
    }

    @Test
    @DisplayName("GET /api/members should return 200 with empty collection when no members exist")
    @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
    void shouldReturnEmptyCollectionWhenNoMembers() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("GET /api/members should return collection of member summaries with HATEOAS links")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
    void shouldReturnMemberCollectionWithHateoasLinks() throws Exception {
        // First, create a member
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest createRequest = new RegisterMemberRequest(
                "Jan",
                "Novák",
                LocalDate.of(2005, 5, 15),
                "CZ",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777123456",
                address,
                null
        );

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                .content(objectMapper.writeValueAsString(createRequest))
                )
                .andExpect(status().isCreated());

        // Then, list members
        mockMvc.perform(
                        get("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList").exists())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("GET /api/members with pagination should return paginated results")
    @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
    void shouldReturnPaginatedResults() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("page", "0")
                                .param("size", "10")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("GET /api/members with sort parameter should return sorted results")
    @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
    void shouldReturnSortedResults() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("sort", "lastName,asc")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    @DisplayName("GET /api/members with invalid sort field should return 400")
    @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
    void shouldReturn400WhenSortFieldInvalid() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("sort", "invalidField,asc")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Invalid sort field")));
    }

    @Test
    @DisplayName("GET /api/members with multi-field sort should work correctly")
    @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
    void shouldHandleMultiFieldSort() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .param("sort", "lastName,asc")
                                .param("sort", "firstName,asc")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").exists());
    }

    @Test
    @DisplayName("GET /api/members should use default pagination when no parameters provided")
    @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
    void shouldUseDefaultPaginationWhenNoParams() throws Exception {
        mockMvc.perform(
                        get("/api/members")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    @DisplayName("GET /api/members should include pagination links")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
    void shouldIncludePaginationLinks() throws Exception {
        // Create multiple members to ensure pagination
        for (int i = 0; i < 3; i++) {
            AddressRequest address = new AddressRequest(
                    "Street " + i,
                    "City " + i,
                    "1000" + i,
                    "CZ"
            );

            RegisterMemberRequest createRequest = new RegisterMemberRequest(
                    "Member" + i,
                    "Test" + i,
                    LocalDate.of(2005, 5, 15),
                    "CZ",
                    Gender.MALE,
                    "member" + i + "@example.com",
                    "+42077712345" + i,
                    address,
                    null
            );

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(createRequest))
                    )
                    .andExpect(status().isCreated());
        }

        // Request with pagination
        mockMvc.perform(
                        get("/api/members")
                                .param("page", "0")
                                .param("size", "2")
                                .contentType("application/json")
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("POST /api/members with invalid address missing fields should return 400")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldReturn400WhenAddressMissingFields() throws Exception {
        AddressRequest address = new AddressRequest(
                "",  // Invalid: empty street
                "Praha",
                "11000",
                "CZ"
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
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

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://klabis.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors['address.street']").exists());
    }

    @Test
    @DisplayName("POST /api/members with invalid address country code format should return 400")
    @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
    void shouldReturn400WhenAddressCountryInvalid() throws Exception {
        AddressRequest address = new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "X"  // Invalid: not 2 letters (fails pattern ^[A-Za-z]{2}$)
        );

        RegisterMemberRequest request = new RegisterMemberRequest(
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

        mockMvc.perform(
                        post("/api/members")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://klabis.com/problems/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors['address.country']").exists());
    }
}
