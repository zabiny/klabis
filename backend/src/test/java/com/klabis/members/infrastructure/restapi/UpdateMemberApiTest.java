package com.klabis.members.infrastructure.restapi;

import com.klabis.config.encryption.EncryptionConfiguration;
import com.klabis.members.domain.*;
import com.klabis.members.management.InvalidUpdateException;
import com.klabis.members.management.ManagementService;
import com.klabis.members.management.MemberNotFoundException;
import com.klabis.members.management.SelfEditNotAllowedException;
import com.klabis.users.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API tests for PATCH /api/members/{id} endpoint.
 * <p>
 * Tests admin edit, member self-edit, authorization, and validation.
 * <p>
 * The PATCH endpoint returns 204 No Content without response body.
 * To verify updated data, clients should call the GET endpoint after PATCH.
 */
@DisplayName("Update Member Controller API Tests")
@WebMvcTest(controllers = MemberController.class)
@Import(EncryptionConfiguration.class)
class UpdateMemberApiTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String MEMBER_EMAIL = "jan.novak@example.com";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagementService memberService;

    @MockitoBean
    private Members memberRepository;

    @MockitoBean
    private MemberMapper memberMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private PagedResourcesAssembler<?> pagedResourcesAssembler;

    private final UUID testMemberId = UUID.randomUUID();

    @Nested
    @DisplayName("PATCH /api/members/{id}")
    class UpdateMemberTests {

        @Nested
        @DisplayName("Admin edit")
        class AdminEditTests {

            @Test
            @DisplayName("updating member email should return 204 No Content")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateMemberEmailWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "email": "new.email@example.com"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());

                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.email()).contains("new.email@example.com");
                assertThat(capturedRequest.phone()).isEmpty();
                assertThat(capturedRequest.address()).isEmpty();
            }

            @Test
            @DisplayName("updating member phone should return 204 No Content")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateMemberPhoneWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "phone": "+420777123456"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());

                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.email()).isEmpty();
                assertThat(capturedRequest.phone()).contains("+420777123456");
                assertThat(capturedRequest.address()).isEmpty();
            }

            @Test
            @DisplayName("updating member address should return 204 No Content")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateMemberAddressWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "address": {
                                                        "street": "New Street 123",
                                                        "city": "Prague",
                                                        "postalCode": "11000",
                                                        "country": "CZ"
                                                    }
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());

                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.email()).isEmpty();
                assertThat(capturedRequest.phone()).isEmpty();
                assertThat(capturedRequest.address()).isPresent();
                assertThat(capturedRequest.address().get().street()).isEqualTo("New Street 123");
                assertThat(capturedRequest.address().get().city()).isEqualTo("Prague");
                assertThat(capturedRequest.address().get().postalCode()).isEqualTo("11000");
                assertThat(capturedRequest.address().get().country()).isEqualTo("CZ");
            }

            @Test
            @DisplayName("updating admin-only fields should return 204 No Content")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateAdminOnlyFieldsWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "gender": "FEMALE",
                                                    "chipNumber": "12345",
                                                    "drivingLicenseGroup": "B",
                                                    "dietaryRestrictions": "Vegetarian"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.gender()).contains(Gender.FEMALE);
                assertThat(capturedRequest.chipNumber()).contains("12345");
                assertThat(capturedRequest.drivingLicenseGroup()).contains(DrivingLicenseGroup.B);
                assertThat(capturedRequest.dietaryRestrictions()).contains("Vegetarian");

            }

            @Test
            @DisplayName("updating birth number and bank account should return 204 No Content")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateBirthNumberAndBankAccountWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "birthNumber": "900101/1234",
                                                    "bankAccountNumber": "CZ6508000000192000145399"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.birthNumber()).contains("900101/1234");
                assertThat(capturedRequest.bankAccountNumber()).contains("CZ6508000000192000145399");

            }

            @Test
            @DisplayName("updating only birth number should return 204 No Content")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateOnlyBirthNumberWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "birthNumber": "850520/9876"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.birthNumber()).contains("850520/9876");
                assertThat(capturedRequest.bankAccountNumber()).isEmpty();

            }

            @Test
            @DisplayName("updating only bank account number should return 204 No Content")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateOnlyBankAccountNumberWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "bankAccountNumber": "CZ6508000000192000145399"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.birthNumber()).isEmpty();
                assertThat(capturedRequest.bankAccountNumber()).contains("CZ6508000000192000145399");

            }

            @Test
            @DisplayName("updating documents should return 204 No Content")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldUpdateDocumentsWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "identityCard": {
                                                        "number": "123456789",
                                                        "validUntil": "%s"
                                                    },
                                                    "medicalCourse": {
                                                        "completedOn": "2024-01-01",
                                                        "validUntil": "%s"
                                                    },
                                                    "trainerLicense": {
                                                        "licenseNumber": "TRAINER123",
                                                        "validUntil": "%s"
                                                    }
                                                }
                                                """.formatted(
                                                        LocalDate.now().plusYears(5),
                                                        LocalDate.now().plusYears(2),
                                                        LocalDate.now().plusYears(3)
                                                ))
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.identityCard()).isPresent();
                assertThat(capturedRequest.medicalCourse()).isPresent();
                assertThat(capturedRequest.trainerLicense()).isPresent();

            }

            @Test
            @DisplayName("performing partial update should return 204 No Content")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldPerformPartialUpdateWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "email": "partial.update@example.com",
                                                    "dietaryRestrictions": "No dairy"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.email()).contains("partial.update@example.com");
                assertThat(capturedRequest.dietaryRestrictions()).contains("No dairy");

            }
        }

        @Nested
        @DisplayName("Member self-edit")
        class MemberSelfEditTests {

            @Test
            @DisplayName("updating own email should return 204 No Content")
            @WithMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateOwnEmail() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "email": "my.new.email@example.com"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.email()).contains("my.new.email@example.com");

            }

            @Test
            @DisplayName("updating own phone should return 204 No Content")
            @WithMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateOwnPhone() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "phone": "+420987654321"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.phone()).contains("+420987654321");

            }

            @Test
            @DisplayName("updating own address should return 204 No Content")
            @WithMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateOwnAddress() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "address": {
                                                        "street": "My New Address 456",
                                                        "city": "Brno",
                                                        "postalCode": "60200",
                                                        "country": "CZ"
                                                    }
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.address()).isPresent();
                assertThat(capturedRequest.address().get().street()).isEqualTo("My New Address 456");
                assertThat(capturedRequest.address().get().city()).isEqualTo("Brno");

            }

            @Test
            @DisplayName("updating dietary restrictions should return 204 No Content")
            @WithMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateDietaryRestrictions() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "dietaryRestrictions": "Gluten-free, no nuts"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
                var captor = forClass(UpdateMemberRequest.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var capturedRequest = captor.getValue();
                assertThat(capturedRequest.dietaryRestrictions()).contains("Gluten-free, no nuts");

            }
        }

        @Nested
        @DisplayName("Authorization")
        class AuthorizationTests {

            @Test
            @DisplayName("non-admin editing another member should return 403")
            @WithMockUser(username = "other.user@example.com", authorities = {})
            void shouldReturn403WhenNonAdminEditsAnotherMember() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenThrow(new SelfEditNotAllowedException(
                                new RegistrationNumber("ZBM9090"), new RegistrationNumber("ZBM1234")));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "email": "hacker@example.com"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.title").value("Authorization Failed"))
                        .andExpect(jsonPath("$.detail").value("User with email 'ZBM9090' is not authorized to edit member with email 'ZBM1234'. Members can only edit their own information."));
            }

            @Test
            @DisplayName("unauthenticated request should return 401")
            void shouldReturn401WhenUnauthenticated() throws Exception {
                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "email": "test@example.com"
                                                }
                                                """)
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
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenThrow(new InvalidUpdateException("Update request must contain at least one field"));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("{}")
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
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenThrow(new InvalidUpdateException("Invalid email format: invalid-email"));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "email": "invalid-email"
                                                }
                                                """)
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
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenThrow(new InvalidUpdateException("Invalid phone format: 123"));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "phone": "123"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("invalid chip number should return 400")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldReturn400WhenChipNumberInvalid() throws Exception {
                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "chipNumber": "ABC123"
                                                }
                                                """)
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

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "dietaryRestrictions": "%s"
                                                }
                                                """.formatted(tooLongRestrictions))
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

                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenThrow(new MemberNotFoundException(new MemberId(nonExistentId)));

                mockMvc.perform(
                                patch("/api/members/{id}", nonExistentId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "email": "test@example.com"
                                                }
                                                """)
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
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenThrow(new OptimisticLockingFailureException("Concurrent update for member - test purpose"));

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "email": "test@example.com"
                                                }
                                                """)
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
            @DisplayName("PATCH should return 204 No Content (HATEOAS links available via GET)")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldReturn204NoContentForPatchWithHalFormsAccept() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                        .content("""
                                                {
                                                    "email": "test@example.com"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
            }

            @Test
            @DisplayName("response should include collection link")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldIncludeCollectionLink() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                        .content("""
                                                {
                                                    "email": "test@example.com"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
            }

            @Test
            @DisplayName("response should include edit link")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldIncludeEditLink() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                        .content("""
                                                {
                                                    "email": "test@example.com"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
            }

            @Test
            @DisplayName("PATCH with multiple fields should return 204 No Content")
            @WithMockUser(username = ADMIN_USERNAME, authorities = {"MEMBERS:UPDATE"})
            void shouldReturn204NoContentWhenUpdatingMultipleFields() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(UpdateMemberRequest.class)))
                        .thenReturn(testMemberId);

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                        .content("""
                                                {
                                                    "email": "updated@example.com",
                                                    "phone": "+420999888777",
                                                    "address": {
                                                        "street": "Updated 123",
                                                        "city": "Updated City",
                                                        "postalCode": "10000",
                                                        "country": "CZ"
                                                    },
                                                    "dietaryRestrictions": "Vegan"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());
            }
        }
    }
}
