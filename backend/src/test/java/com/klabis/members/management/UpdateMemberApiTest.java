package com.klabis.members.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.SecurityTestBase;
import com.klabis.members.DrivingLicenseGroup;
import com.klabis.members.Gender;
import com.klabis.members.RegistrationNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.hateoas.MediaTypes;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for PATCH /api/members/{id} endpoint.
 * <p>
 * Tests admin edit, member self-edit, authorization, validation, and HATEOAS link generation.
 */
@DisplayName("Update Member Controller API Tests")
@ApplicationModuleTest(extraIncludes = {"users", "common"}, mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@Import(TestApplicationConfiguration.class)
class UpdateMemberApiTest extends SecurityTestBase {

    @MockitoBean
    private ManagementService memberService;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID testMemberId = UUID.randomUUID();
    private final String memberEmail = "jan.novak@example.com";

    @Nested
    @DisplayName("Admin Edit Tests - with MEMBERS:UPDATE authority")
    class AdminEditTests {

        @Test
        @DisplayName("Admin updating member's email should return 200 with updated member")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldUpdateMemberEmailWhenAdmin() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("new.email@example.com"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createMemberDetailsDTO(testMemberId,
                    "new.email@example.com"));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                    .andExpect(jsonPath("$.id").value(testMemberId.toString()))
                    .andExpect(jsonPath("$.email").value("new.email@example.com"))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").exists())
                    .andExpect(jsonPath("$._links.edit.href").exists());

            verify(memberService).updateMember(any(UUID.class), any(UpdateMemberRequest.class));
        }

        @Test
        @DisplayName("Admin updating member's phone should return 200 with updated member")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldUpdateMemberPhoneWhenAdmin() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.of("+420777123456"),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createUpdatedMemberDTO(request, testMemberId));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phone").value("+420777123456"));
        }

        @Test
        @DisplayName("Admin updating member's address should return 200 with updated member")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldUpdateMemberAddressWhenAdmin() throws Exception {
            // Given
            AddressRequest newAddress = new AddressRequest(
                    "New Street 123",
                    "Prague",
                    "11000",
                    "CZ"
            );

            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(newAddress),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createUpdatedMemberDTO(request, testMemberId));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.address.street").value("New Street 123"))
                    .andExpect(jsonPath("$.address.city").value("Prague"))
                    .andExpect(jsonPath("$.address.postalCode").value("11000"))
                    .andExpect(jsonPath("$.address.country").value("CZ"));
        }

        @Test
        @DisplayName("Admin updating admin-only fields (gender, chipNumber, drivingLicenseGroup, dietaryRestrictions) should return 200")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldUpdateAdminOnlyFieldsWhenAdmin() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.of(Gender.FEMALE),
                    Optional.of("12345"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(DrivingLicenseGroup.B),
                    Optional.of("Vegetarian")
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createUpdatedMemberDTO(request, testMemberId));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gender").value("FEMALE"))
                    .andExpect(jsonPath("$.chipNumber").value("12345"))
                    .andExpect(jsonPath("$.drivingLicenseGroup").value("B"))
                    .andExpect(jsonPath("$.dietaryRestrictions").value("Vegetarian"));
        }

        @Test
        @DisplayName("Admin updating documents (identityCard, medicalCourse, trainerLicense) should return 200")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldUpdateDocumentsWhenAdmin() throws Exception {
            // Given
            IdentityCardDto identityCard = new IdentityCardDto("123456789", LocalDate.now().plusYears(5));
            MedicalCourseDto medicalCourse = new MedicalCourseDto(LocalDate.of(2024, 1, 1),
                    Optional.of(LocalDate.now().plusYears(2)));
            TrainerLicenseDto trainerLicense = new TrainerLicenseDto("TRAINER123", LocalDate.now().plusYears(3));

            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(identityCard),
                    Optional.of(medicalCourse),
                    Optional.of(trainerLicense),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createUpdatedMemberDTO(request, testMemberId));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.identityCard.cardNumber").value("123456789"))
                    .andExpect(jsonPath("$.medicalCourse.completionDate").exists())
                    .andExpect(jsonPath("$.trainerLicense.licenseNumber").value("TRAINER123"));
        }

        @Test
        @DisplayName("Admin performing partial update (only some fields) should return 200")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldPerformPartialUpdateWhenAdmin() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("partial.update@example.com"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("No dairy")
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createUpdatedMemberDTO(request, testMemberId));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("partial.update@example.com"))
                    .andExpect(jsonPath("$.dietaryRestrictions").value("No dairy"));
        }
    }

    @Nested
    @DisplayName("Member Self-Edit Tests - member editing own information")
    class MemberSelfEditTests {

        @Test
        @DisplayName("Member updating their own email should return 200")
        @WithMockUser(username = memberEmail, authorities = {})
            // No MEMBERS:UPDATE authority
        void shouldAllowMemberToUpdateOwnEmail() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("my.new.email@example.com"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createMemberDetailsDTO(testMemberId,
                    "my.new.email@example.com"));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("my.new.email@example.com"));
        }

        @Test
        @DisplayName("Member updating their own phone should return 200")
        @WithMockUser(username = memberEmail, authorities = {})
        void shouldAllowMemberToUpdateOwnPhone() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.of("+420987654321"),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createUpdatedMemberDTO(request, testMemberId));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phone").value("+420987654321"));
        }

        @Test
        @DisplayName("Member updating their own address should return 200")
        @WithMockUser(username = memberEmail, authorities = {})
        void shouldAllowMemberToUpdateOwnAddress() throws Exception {
            // Given
            AddressRequest newAddress = new AddressRequest(
                    "My New Address 456",
                    "Brno",
                    "60200",
                    "CZ"
            );

            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(newAddress),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createUpdatedMemberDTO(request, testMemberId));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.address.street").value("My New Address 456"))
                    .andExpect(jsonPath("$.address.city").value("Brno"));
        }

        @Test
        @DisplayName("Member updating dietary restrictions should return 200")
        @WithMockUser(username = memberEmail, authorities = {})
        void shouldAllowMemberToUpdateDietaryRestrictions() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("Gluten-free, no nuts")
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createUpdatedMemberDTO(request, testMemberId));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dietaryRestrictions").value("Gluten-free, no nuts"));
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Non-admin trying to edit another member should return 403")
        @WithMockUser(username = "other.user@example.com", authorities = {})
        void shouldReturn403WhenNonAdminEditsAnotherMember() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("hacker@example.com"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                    .thenThrow(new SelfEditNotAllowedException(
                            new RegistrationNumber("ZBM9090"), new RegistrationNumber("ZBM1234")));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Self-Edit Not Allowed"));
        }

        @Test
        @DisplayName("Unauthenticated request should return 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("test@example.com"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Empty update (no fields) should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldReturn400WhenUpdateIsEmpty() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                    .thenThrow(new InvalidUpdateException(
                            "Update request must contain at least one field"));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Update"));
        }

        @Test
        @DisplayName("Invalid email format should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldReturn400WhenEmailInvalid() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("invalid-email"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                    .thenThrow(new InvalidUpdateException(
                            "Invalid email format: invalid-email"));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Update"));
        }

        @Test
        @DisplayName("Invalid phone format should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldReturn400WhenPhoneInvalid() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.of("123"),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                    .thenThrow(new InvalidUpdateException(
                            "Invalid phone format: 123"));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Invalid chip number (non-numeric) should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldReturn400WhenChipNumberInvalid() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.of("ABC123"), // Invalid: non-numeric
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"));
        }

        @Test
        @DisplayName("Invalid dietary restrictions (too long) should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldReturn400WhenDietaryRestrictionsTooLong() throws Exception {
            // Given
            String tooLongRestrictions = "A".repeat(501); // Exceeds 500 characters

            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(tooLongRestrictions)
            );

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Non-existent member should return 404")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldReturn404WhenMemberNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("test@example.com"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                    .thenThrow(new MemberNotFoundException(nonExistentId));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", nonExistentId)
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Member Not Found"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(nonExistentId.toString())));
        }

        @Test
        @DisplayName("Concurrent update should return 409 (optimistic locking)")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldReturn409WhenConcurrentUpdate() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("test@example.com"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                    .thenThrow(new OptimisticLockingFailureException("Concurrent modification detected"));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Concurrent Update Conflict"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                            "modified by another user")));
        }
    }

    @Nested
    @DisplayName("HAL+FORMS Tests")
    class HateoasTests {

        @Test
        @DisplayName("Response should include self link")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldIncludeSelfLink() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("test@example.com"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createMemberDetailsDTO(testMemberId, memberEmail));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString("/api/members/" + testMemberId)));
        }

        @Test
        @DisplayName("Response should include collection link")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldIncludeCollectionLink() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("test@example.com"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createMemberDetailsDTO(testMemberId, memberEmail));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.collection.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").value(org.hamcrest.Matchers.containsString(
                            "/api/members")));
        }

        @Test
        @DisplayName("Response should include edit link")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldIncludeEditLink() throws Exception {
            // Given
            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("test@example.com"),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createMemberDetailsDTO(testMemberId, memberEmail));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.edit.href").exists());
        }

        @Test
        @DisplayName("Response should include all updated fields")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
        void shouldIncludeAllUpdatedFields() throws Exception {
            // Given
            AddressRequest newAddress = new AddressRequest("Updated 123", "Updated City", "10000", "CZ");

            UpdateMemberRequest request = new UpdateMemberRequest(
                    Optional.of("updated@example.com"),
                    Optional.of("+420999888777"),
                    Optional.of(newAddress),
                    Optional.empty(),  // firstName
                    Optional.empty(),  // lastName
                    Optional.empty(),  // dateOfBirth
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of("Vegan")
            );

            when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class))).thenReturn(testMemberId);
            when(memberService.getMember(any())).thenReturn(createUpdatedMemberDTO(request, testMemberId));

            // When & Then
            mockMvc.perform(
                            patch("/api/members/{id}", testMemberId)
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("updated@example.com"))
                    .andExpect(jsonPath("$.phone").value("+420999888777"))
                    .andExpect(jsonPath("$.dietaryRestrictions").value("Vegan"))
                    .andExpect(jsonPath("$.address.street").value("Updated 123"))
                    .andExpect(jsonPath("$.address.city").value("Updated City"));
        }
    }

    // Helper methods

    private MemberDetailsDTO createMemberDetailsDTO(UUID id, String email) {
        return createMemberDetailsDTO(id, email, "+420777888999", null, null);
    }

    private MemberDetailsDTO createMemberDetailsDTO(UUID id, String email, String phone, String dietaryRestrictions) {
        return createMemberDetailsDTO(id, email, phone, dietaryRestrictions, null);
    }

    private MemberDetailsDTO createMemberDetailsDTO(UUID id, String email, String phone, String dietaryRestrictions, AddressResponse address) {
        AddressResponse defaultAddress = address != null ? address : new AddressResponse(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );

        return new MemberDetailsDTO(
                id,
                "ZBM0501",
                "Jan",
                "Novák",
                LocalDate.of(2005, 6, 15),
                "CZ",
                Gender.MALE,
                email,
                phone,
                defaultAddress,
                null,
                true,
                null, // chipNumber
                null, // identityCard
                null, // medicalCourse
                null, // trainerLicense
                null, // drivingLicenseGroup
                dietaryRestrictions
        );
    }

    private MemberDetailsDTO createUpdatedMemberDTO(UpdateMemberRequest request, UUID memberId) {
        // Build a MemberDetailsDTO that reflects the updates from the request
        String email = request.email().orElse("jan.novak@example.com");
        String phone = request.phone().orElse("+420777888999");
        String dietaryRestrictions = request.dietaryRestrictions().orElse(null);
        Gender gender = request.gender().orElse(Gender.MALE);
        String chipNumber = request.chipNumber().orElse(null);
        DrivingLicenseGroup drivingLicenseGroup = request.drivingLicenseGroup().orElse(null);

        AddressResponse address = request.address()
                .map(addr -> new AddressResponse(addr.street(), addr.city(), addr.postalCode(), addr.country()))
                .orElse(null);

        IdentityCardDto identityCard = request.identityCard().orElse(null);
        MedicalCourseDto medicalCourse = request.medicalCourse().orElse(null);
        TrainerLicenseDto trainerLicense = request.trainerLicense().orElse(null);

        AddressResponse finalAddress = address != null ? address : new AddressResponse("Hlavní 123",
                "Praha",
                "11000",
                "CZ");

        return new MemberDetailsDTO(
                memberId,
                "ZBM0501",
                "Jan",
                "Novák",
                LocalDate.of(2005, 6, 15),
                "CZ",
                gender,
                email,
                phone,
                finalAddress,
                null,
                true,
                chipNumber,
                identityCard,
                medicalCourse,
                trainerLicense,
                drivingLicenseGroup,
                dietaryRestrictions
        );
    }
}
