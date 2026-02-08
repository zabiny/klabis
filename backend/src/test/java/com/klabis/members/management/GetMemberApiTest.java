package com.klabis.members.management;

import com.klabis.TestApplicationConfiguration;
import com.klabis.common.SecurityTestBase;
import com.klabis.members.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for GET /api/members/{id} endpoint.
 */
@DisplayName("Get Member Controller API Tests")
@ApplicationModuleTest(extraIncludes = {"users", "common"}, mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
// need users for Security configuration, common for EmailService
@Import(TestApplicationConfiguration.class)
class GetMemberApiTest extends SecurityTestBase {

    @MockitoBean
    private ManagementService memberService;

    @Test
    @DisplayName("GET /api/members/{id} should return 200 with member details")
    @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
    void shouldReturnMemberDetailsWhenFound() throws Exception {
        // Given
        UUID memberId = UUID.randomUUID();
        AddressResponse address = new AddressResponse(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        MemberDetailsDTO memberDTO = new MemberDetailsDTO(
                memberId,
                "ZBM0501",
                "Jan",
                "Novák",
                LocalDate.of(2005, 6, 15),
                "CZ",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777888999",
                address,
                null,
                true,
                null, // chipNumber
                null, // identityCard
                null, // medicalCourse
                null, // trainerLicense
                null, // drivingLicenseGroup
                null  // dietaryRestrictions
        );

        when(memberService.getMember(any())).thenReturn(memberDTO);

        // When & Then
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(memberId.toString()))
                .andExpect(jsonPath("$.registrationNumber").value("ZBM0501"))
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
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$._links.self.href").exists())
                .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString("/api/members/" + memberId)))
                .andExpect(jsonPath("$._links.collection.href").exists())
                .andExpect(jsonPath("$._links.collection.href").value(org.hamcrest.Matchers.containsString(
                        "/api/members")));
    }

    @Test
    @DisplayName("GET /api/members/{id} should return 404 when member not found")
    @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
    void shouldReturn404WhenMemberNotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(memberService.getMember(any()))
                .thenThrow(new MemberNotFoundException(nonExistentId));

        // When & Then
        mockMvc.perform(
                        get("/api/members/{id}", nonExistentId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(nonExistentId.toString())));
    }

    @Test
    @DisplayName("GET /api/members/{id} should return 403 when user lacks MEMBERS:READ authority")
    @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:CREATE"})
    void shouldReturn403WhenUnauthorized() throws Exception {
        // Given
        UUID memberId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/members/{id} should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        // Given
        UUID memberId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/members/{id} should include edit link when user has MEMBERS:UPDATE authority")
    @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ", "MEMBERS:UPDATE"})
    void shouldIncludeEditLinkWhenUserHasUpdateAuthority() throws Exception {
        // Given
        UUID memberId = UUID.randomUUID();
        AddressResponse address = new AddressResponse(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        MemberDetailsDTO memberDTO = new MemberDetailsDTO(
                memberId,
                "ZBM0501",
                "Jan",
                "Novák",
                LocalDate.of(2005, 6, 15),
                "CZ",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777888999",
                address,
                null,
                true,
                null, // chipNumber
                null, // identityCard
                null, // medicalCourse
                null, // trainerLicense
                null, // drivingLicenseGroup
                null  // dietaryRestrictions
        );

        when(memberService.getMember(any())).thenReturn(memberDTO);

        // When & Then
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.edit.href").exists());
    }

    @Test
    @DisplayName("GET /api/members/{id} should return guardian information when present")
    @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
    void shouldReturnGuardianInformationWhenPresent() throws Exception {
        // Given
        UUID memberId = UUID.randomUUID();
        GuardianDTO guardianDTO = new GuardianDTO(
                "Parent",
                "Name",
                "PARENT",
                "parent@example.com",
                "+420777111222"
        );

        AddressResponse address = new AddressResponse(
                "Hlavní 456",
                "Brno",
                "60000",
                "CZ"
        );

        MemberDetailsDTO memberDTO = new MemberDetailsDTO(
                memberId,
                "ZBM1501",
                "Child",
                "Member",
                LocalDate.of(2015, 1, 10),
                "CZ",
                Gender.MALE,
                "child@example.com",
                "+420777333444",
                address,
                guardianDTO,
                true,
                null, // chipNumber
                null, // identityCard
                null, // medicalCourse
                null, // trainerLicense
                null, // drivingLicenseGroup
                null  // dietaryRestrictions
        );

        when(memberService.getMember(any())).thenReturn(memberDTO);

        // When & Then
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("child@example.com"))
                .andExpect(jsonPath("$.phone").value("+420777333444"))
                .andExpect(jsonPath("$.address.street").value("Hlavní 456"))
                .andExpect(jsonPath("$.address.city").value("Brno"))
                .andExpect(jsonPath("$.guardian").isNotEmpty())
                .andExpect(jsonPath("$.guardian.firstName").value("Parent"))
                .andExpect(jsonPath("$.guardian.lastName").value("Name"))
                .andExpect(jsonPath("$.guardian.relationship").value("PARENT"))
                .andExpect(jsonPath("$.guardian.email").value("parent@example.com"))
                .andExpect(jsonPath("$.guardian.phone").value("+420777111222"));
    }

    @Test
    @DisplayName("GET /api/members/{id} should return single email and phone with address")
    @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
    void shouldReturnSingleEmailAndPhoneWithAddress() throws Exception {
        // Given
        UUID memberId = UUID.randomUUID();
        AddressResponse address = new AddressResponse(
                "Main Street 123",
                "Bratislava",
                "81101",
                "SK"
        );

        MemberDetailsDTO memberDTO = new MemberDetailsDTO(
                memberId,
                "ZBM0501",
                "Eva",
                "Svobodová",
                LocalDate.of(2010, 3, 20),
                "SK",
                Gender.FEMALE,
                "eva.svobodova@example.com",
                "+421777888999",
                address,
                null,
                true,
                null, // chipNumber
                null, // identityCard
                null, // medicalCourse
                null, // trainerLicense
                null, // drivingLicenseGroup
                null  // dietaryRestrictions
        );

        when(memberService.getMember(any())).thenReturn(memberDTO);

        // When & Then
        mockMvc.perform(
                        get("/api/members/{id}", memberId)
                                .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("eva.svobodova@example.com"))
                .andExpect(jsonPath("$.phone").value("+421777888999"))
                .andExpect(jsonPath("$.address.street").value("Main Street 123"))
                .andExpect(jsonPath("$.address.city").value("Bratislava"))
                .andExpect(jsonPath("$.address.postalCode").value("81101"))
                .andExpect(jsonPath("$.address.country").value("SK"));
    }
}
