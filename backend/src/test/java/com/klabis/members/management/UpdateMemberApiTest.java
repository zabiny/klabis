package com.klabis.members.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.config.encryption.EncryptionConfiguration;
import com.klabis.members.*;
import com.klabis.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
@WebMvcTest(controllers = MemberController.class)
@Import({EncryptionConfiguration.class, MemberMapperImpl.class})
class UpdateMemberApiTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String MEMBER_EMAIL = "jan.novak@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManagementService memberService;

    @MockitoBean
    private Members memberRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private final UUID testMemberId = UUID.randomUUID();

    @Nested
    @DisplayName("PATCH /api/members/{id}")
    class UpdateMemberTests {

        @Nested
        @DisplayName("Admin edit")
        class AdminEditTests {

            @Test
            @DisplayName("updating member email should return 200 with updated member")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateMemberEmailWhenAdmin() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("new.email@example.com"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class)))
                        .thenReturn(Optional.of(createTestMember(testMemberId, "new.email@example.com")));

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
                        .andExpect(jsonPath("$._templates.default.method").value("PATCH")); // EDIT

                verify(memberService).updateMember(any(UUID.class), any(UpdateMemberRequest.class));
            }

            @Test
            @DisplayName("updating member phone should return 200 with updated member")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateMemberPhoneWhenAdmin() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.of("+420777123456"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

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
            @DisplayName("updating member address should return 200 with updated member")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateMemberAddressWhenAdmin() throws Exception {
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
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

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
            @DisplayName("updating admin-only fields should return 200")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateAdminOnlyFieldsWhenAdmin() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(Gender.FEMALE),
                        Optional.of("12345"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(DrivingLicenseGroup.B),
                        Optional.of("Vegetarian"),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

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
            @DisplayName("updating birth number and bank account should return 200")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateBirthNumberAndBankAccountWhenAdmin() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("900101/1234"),
                        Optional.of("CZ6508000000192000145399")
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.birthNumber").value("900101/1234"))
                        .andExpect(jsonPath("$.bankAccountNumber").value("CZ6508000000192000145399"));
            }

            @Test
            @DisplayName("updating only birth number should return 200")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateOnlyBirthNumberWhenAdmin() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("850520/9876"),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.birthNumber").value("850520/9876"));
            }

            @Test
            @DisplayName("updating only bank account number should return 200")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateOnlyBankAccountNumberWhenAdmin() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("CZ6508000000192000145399")
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.bankAccountNumber").value("CZ6508000000192000145399"));
            }

            @Test
            @DisplayName("updating documents should return 200")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateDocumentsWhenAdmin() throws Exception {
                IdentityCardDto identityCard = new IdentityCardDto("123456789", LocalDate.now().plusYears(5));
                MedicalCourseDto medicalCourse = new MedicalCourseDto(LocalDate.of(2024, 1, 1),
                        Optional.of(LocalDate.now().plusYears(2)));
                TrainerLicenseDto trainerLicense = new TrainerLicenseDto("TRAINER123", LocalDate.now().plusYears(3));

                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(identityCard),
                        Optional.of(medicalCourse),
                        Optional.of(trainerLicense),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

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
            @DisplayName("performing partial update should return 200")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldPerformPartialUpdateWhenAdmin() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("partial.update@example.com"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("No dairy"),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

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
        @DisplayName("Member self-edit")
        class MemberSelfEditTests {

            @Test
            @DisplayName("updating own email should return 200")
            @WithMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateOwnEmail() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("my.new.email@example.com"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createTestMember(testMemberId,
                        "my.new.email@example.com")));

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
            @DisplayName("updating own phone should return 200")
            @WithMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateOwnPhone() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.of("+420987654321"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

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
            @DisplayName("updating own address should return 200")
            @WithMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateOwnAddress() throws Exception {
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
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

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
            @DisplayName("updating dietary restrictions should return 200")
            @WithMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateDietaryRestrictions() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("Gluten-free, no nuts"),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

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
        @DisplayName("Authorization")
        class AuthorizationTests {

            @Test
            @DisplayName("non-admin editing another member should return 403")
            @WithMockUser(username = "other.user@example.com", authorities = {})
            void shouldReturn403WhenNonAdminEditsAnotherMember() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("hacker@example.com"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
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

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.title").value("Authorization Failed"))
                        .andExpect(jsonPath("$.detail").value("User with email 'ZBM9090' is not authorized to edit member with email 'ZBM1234'. Members can only edit their own information."));
            }

            @Test
            @DisplayName("unauthenticated request should return 401")
            void shouldReturn401WhenUnauthenticated() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("test@example.com"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

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
        @DisplayName("Validation")
        class ValidationTests {

            @Test
            @DisplayName("empty update should return 400")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldReturn400WhenUpdateIsEmpty() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenThrow(new InvalidUpdateException("Update request must contain at least one field"));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.detail").value("Update request must contain at least one field"))
                        .andExpect(jsonPath("$.title").value("Bad Request"));
            }

            @Test
            @DisplayName("invalid email format should return 400")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldReturn400WhenEmailInvalid() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("invalid-email"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenThrow(new InvalidUpdateException("Invalid email format: invalid-email"));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.detail").value("Invalid email format: invalid-email"))
                        .andExpect(jsonPath("$.title").value("Bad Request"));
            }

            @Test
            @DisplayName("invalid phone format should return 400")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldReturn400WhenPhoneInvalid() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.of("123"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenThrow(new InvalidUpdateException("Invalid phone format: 123"));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("invalid chip number should return 400")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldReturn400WhenChipNumberInvalid() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("ABC123"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.title").value("Bad Request"));
            }

            @Test
            @DisplayName("dietary restrictions too long should return 400")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldReturn400WhenDietaryRestrictionsTooLong() throws Exception {
                String tooLongRestrictions = "A".repeat(501);

                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(tooLongRestrictions),
                        Optional.empty(),
                        Optional.empty()
                );

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.title").value("Bad Request"));
            }
        }

        @Nested
        @DisplayName("Error handling")
        class ErrorHandlingTests {

            @Test
            @DisplayName("non-existent member should return 404")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldReturn404WhenMemberNotFound() throws Exception {
                UUID nonExistentId = UUID.randomUUID();
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("test@example.com"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
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

                mockMvc.perform(
                                patch("/api/members/{id}", nonExistentId)
                                        .contentType("application/json")
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.title").value("Resource Not Found"))
                        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                                nonExistentId.toString())));
            }

            @Test
            @DisplayName("concurrent update should return 409")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldReturn409WhenConcurrentUpdate() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("test@example.com"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenThrow(new OptimisticLockingFailureException("Concurrent update for member - test purpose"));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.title").value("Concurrent Update Conflict"))
                        .andExpect(jsonPath("$.detail").value("Concurrent update for member - test purpose"));
            }
        }

        @Nested
        @DisplayName("HAL+FORMS")
        class HateoasTests {

            @Test
            @DisplayName("response should include self link")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldIncludeSelfLink() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("test@example.com"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createTestMember(testMemberId, MEMBER_EMAIL)));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._links.self.href").exists())
                        .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString(
                                "/api/members/" + testMemberId)));
            }

            @Test
            @DisplayName("response should include collection link")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldIncludeCollectionLink() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("test@example.com"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createTestMember(testMemberId, MEMBER_EMAIL)));

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
            @DisplayName("response should include edit link")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldIncludeEditLink() throws Exception {
                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("test@example.com"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createTestMember(testMemberId, MEMBER_EMAIL)));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                        .content(objectMapper.writeValueAsString(request))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$._templates.default.method").value("PATCH")); // EDIT
            }

            @Test
            @DisplayName("response should include all updated fields")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldIncludeAllUpdatedFields() throws Exception {
                AddressRequest newAddress = new AddressRequest("Updated 123", "Updated City", "10000", "CZ");

                UpdateMemberRequest request = new UpdateMemberRequest(
                        Optional.of("updated@example.com"),
                        Optional.of("+420999888777"),
                        Optional.of(newAddress),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("Vegan"),
                        Optional.empty(),
                        Optional.empty()
                );

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);
                when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(createUpdatedTestMember(request, testMemberId)));

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
    }

    // Helper method to create basic test member
    private Member createTestMember(UUID id, String email) {
        return MemberTestDataBuilder.aMemberWithId(id)
                .withRegistrationNumber("ZBM0501")
                .withName("Jan", "Novák")
                .withDateOfBirth(LocalDate.of(2005, 6, 15))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                .withEmail(email)
                .withPhone("+420777888999")
                .withNoGuardian()
                .build();
    }

    // Helper method to create Member with updated data from request
    private Member createUpdatedTestMember(UpdateMemberRequest request, UUID memberId) {
        String email = request.email().orElse("jan.novak@example.com");
        String phoneValue = request.phone().orElse("+420777888999");
        Gender gender = request.gender().orElse(Gender.MALE);

        Address address = request.address()
                .map(addr -> Address.of(addr.street(), addr.city(), addr.postalCode(), addr.country()))
                .orElse(Address.of("Hlavní 123", "Praha", "11000", "CZ"));

        MemberTestDataBuilder builder = MemberTestDataBuilder.aMemberWithId(memberId)
                .withRegistrationNumber("ZBM0501")
                .withName("Jan", "Novák")
                .withDateOfBirth(LocalDate.of(2005, 6, 15))
                .withNationality("CZ")
                .withGender(gender)
                .withAddress(address)
                .withEmail(email)
                .withPhone(phoneValue)
                .withNoGuardian();

        // Add optional fields from request
        request.chipNumber().ifPresent(builder::withChipNumber);
        request.drivingLicenseGroup().ifPresent(builder::withDrivingLicenseGroup);
        request.dietaryRestrictions().ifPresent(builder::withDietaryRestrictions);
        request.birthNumber().ifPresent(builder::withBirthNumber);
        request.bankAccountNumber().ifPresent(builder::withBankAccountNumber);

        request.identityCard().ifPresent(dto ->
            builder.withIdentityCard(IdentityCard.of(dto.cardNumber(), dto.validityDate())));

        request.medicalCourse().ifPresent(dto ->
            builder.withMedicalCourse(MedicalCourse.of(dto.completionDate(), dto.validityDate())));

        request.trainerLicense().ifPresent(dto ->
            builder.withTrainerLicense(TrainerLicense.of(dto.licenseNumber(), dto.validityDate())));

        return builder.build();
    }
}
