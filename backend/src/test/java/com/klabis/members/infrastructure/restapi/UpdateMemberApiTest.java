package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.encryption.EncryptionConfiguration;
import com.klabis.common.users.Authority;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.*;
import com.klabis.members.management.InvalidUpdateException;
import com.klabis.members.management.ManagementService;
import com.klabis.members.management.MemberNotFoundException;
import com.klabis.members.management.SelfEditNotAllowedException;
import org.junit.jupiter.api.Disabled;
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
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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

    @TestBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private PagedResourcesAssembler<?> pagedResourcesAssembler;

    private final UUID testMemberId = UUID.randomUUID();

    static UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    private Member stubMember() {
        return MemberTestDataBuilder.aMember()
                .withId(testMemberId)
                .withFirstName("Jan")
                .withLastName("Novak")
                .withRegistrationNumber("ZBM1234")
                .withEmail("jan.novak@example.com")
                .withPhone("+420777123456")
                .withAddress(Address.of("Hlavní 1", "Praha", "11000", "CZ"))
                .withNoGuardian()
                .build();
    }

    @Nested
    @DisplayName("PATCH /api/members/{id}")
    class UpdateMemberTests {

        @Nested
        @DisplayName("Admin edit")
        class AdminEditTests {

            @Test
            @DisplayName("updating member email should return 204 No Content")
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldUpdateMemberEmailWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.email()).isEqualTo(EmailAddress.of("new.email@example.com"));
                assertThat(command.phone()).isNull();
                assertThat(command.address()).isNull();
            }

            @Test
            @DisplayName("updating member phone should return 204 No Content")
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldUpdateMemberPhoneWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.email()).isNull();
                assertThat(command.phone()).isEqualTo(PhoneNumber.of("+420777123456"));
                assertThat(command.address()).isNull();
            }

            @Test
            @DisplayName("updating member address should return 204 No Content")
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldUpdateMemberAddressWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.email()).isNull();
                assertThat(command.phone()).isNull();
                assertThat(command.address()).isNotNull();
                assertThat(command.address().street()).isEqualTo("New Street 123");
                assertThat(command.address().city()).isEqualTo("Prague");
                assertThat(command.address().postalCode()).isEqualTo("11000");
                assertThat(command.address().country()).isEqualTo("CZ");
            }

            @Test
            @DisplayName("updating admin-only fields should return 204 No Content")
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldUpdateAdminOnlyFieldsWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.gender()).isEqualTo(Gender.FEMALE);
                assertThat(command.chipNumber()).isEqualTo("12345");
                assertThat(command.drivingLicenseGroup()).isEqualTo(DrivingLicenseGroup.B);
                assertThat(command.dietaryRestrictions()).isEqualTo("Vegetarian");
            }

            @Test
            @DisplayName("updating admin-only fields without MEMBERS:UPDATE authority should return 403")
            @Disabled("Added as new test, probably would rather like 403 if someone attempts to edit (To be changed in prod code)")
            @WithKlabisMockUser(authorities = {})
            void shouldRejectUpdateAdminOnlyFieldsWithoutAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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
                        .andExpect(status().isForbidden());

                verify(memberService, never()).updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class));
            }

            @Test
            @DisplayName("updating birth number and bank account should return 204 No Content")
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldUpdateBirthNumberAndBankAccountWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "birthNumber": "900101/1234",
                                                    "bankAccountNumber": "12345/5678"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.birthNumber()).isEqualTo(BirthNumber.of("900101/1234"));
                assertThat(command.bankAccountNumber()).isEqualTo(BankAccountNumber.of("12345/5678"));
            }

            @Test
            @DisplayName("updating only birth number should return 204 No Content")
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldUpdateOnlyBirthNumberWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.birthNumber()).isEqualTo(BirthNumber.of("850520/9876"));
                assertThat(command.bankAccountNumber()).isNull();
            }

            @Test
            @DisplayName("updating only bank account number should return 204 No Content")
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldUpdateOnlyBankAccountNumberWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "bankAccountNumber": "12345/5678"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isNoContent());

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.birthNumber()).isNull();
                assertThat(command.bankAccountNumber()).isEqualTo(BankAccountNumber.of("12345/5678"));
            }

            @Test
            @DisplayName("updating documents should return 204 No Content")
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldUpdateDocumentsWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

                mockMvc.perform(
                                patch("/api/members/{id}", testMemberId)
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "identityCard": {
                                                        "cardNumber": "123456789",
                                                        "validityDate": "%s"
                                                    },
                                                    "medicalCourse": {
                                                        "completionDate": "2024-01-01",
                                                        "validityDate": "%s"
                                                    },
                                                    "trainerLicense": {
                                                        "licenseNumber": "TRAINER123",
                                                        "validityDate": "%s"
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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.identityCard()).isNotNull();
                assertThat(command.medicalCourse()).isNotNull();
                assertThat(command.trainerLicense()).isNotNull();
            }

            @Test
            @DisplayName("performing partial update should return 204 No Content")
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldPerformPartialUpdateWhenAdmin() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.email()).isEqualTo(EmailAddress.of("partial.update@example.com"));
                assertThat(command.dietaryRestrictions()).isEqualTo("No dairy");
            }
        }

        @Nested
        @DisplayName("Member self-edit")
        class MemberSelfEditTests {

            @Test
            @DisplayName("updating own email should return 204 No Content")
            @WithKlabisMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateOwnEmail() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.email()).isEqualTo(EmailAddress.of("my.new.email@example.com"));
            }

            @Test
            @DisplayName("updating own phone should return 204 No Content")
            @WithKlabisMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateOwnPhone() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.phone()).isEqualTo(PhoneNumber.of("+420987654321"));
            }

            @Test
            @DisplayName("updating own address should return 204 No Content")
            @WithKlabisMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateOwnAddress() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.address()).isNotNull();
                assertThat(command.address().street()).isEqualTo("My New Address 456");
                assertThat(command.address().city()).isEqualTo("Brno");
            }

            @Test
            @DisplayName("updating dietary restrictions should return 204 No Content")
            @WithKlabisMockUser(username = MEMBER_EMAIL, authorities = {})
            void shouldAllowMemberToUpdateDietaryRestrictions() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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

                var captor = forClass(Member.UpdateMemberByAdmin.class);
                verify(memberService).updateMember(any(UUID.class), captor.capture());

                var command = captor.getValue();
                assertThat(command.dietaryRestrictions()).isEqualTo("Gluten-free, no nuts");
            }

            @Test
            @DisplayName("updating items editable only by member himself without beeing that member should return 403 Forbidden")
            @WithKlabisMockUser(userId = "729d897a-5676-431f-b22f-ad935af525ce", authorities = {})
            @Disabled("Added as new test, probably would rather like 403 if someone attempts to edit (To be changed in prod code)")
            void shouldRejectToUpdateSelfEditInformationToOtherMembers() throws Exception {
                mockMvc.perform(
                                patch("/api/members/{id}", "16a9dfbb-145d-4587-b5b5-31383c1d4215")
                                        .contentType("application/json")
                                        .content("""
                                                {
                                                    "dietaryRestrictions": "Gluten-free, no nuts",
                                                    "email": "test@email.com",
                                                    "phone": "+420 123 456 789"
                                                }
                                                """)
                        )
                        .andDo(MockMvcResultHandlers.print())
                        .andExpect(status().isForbidden());

                verify(memberService, never()).updateMember(any(UUID.class), any(Member.SelfUpdate.class));
            }
        }


        @Nested
        @DisplayName("Authorization")
        class AuthorizationTests {

            @Test
            @DisplayName("non-admin editing another member should return 403")
            @WithKlabisMockUser(username = "other.user@example.com", authorities = {})
            void shouldReturn403WhenNonAdminEditsAnotherMember() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
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
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldReturn400WhenUpdateIsEmpty() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
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
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldReturn400WhenEmailInvalid() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
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
                        .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Invalid email")))
                        .andExpect(jsonPath("$.title").value("Bad Request"));
            }

            @Test
            @DisplayName("invalid phone format should return 400")
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldReturn400WhenPhoneInvalid() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
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
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
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
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
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
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldReturn404WhenMemberNotFound() throws Exception {
                UUID nonExistentId = UUID.randomUUID();

                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
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
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldReturn409WhenConcurrentUpdate() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
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
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldReturn204NoContentForPatchWithHalFormsAccept() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldIncludeCollectionLink() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldIncludeEditLink() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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
            @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
            void shouldReturn204NoContentWhenUpdatingMultipleFields() throws Exception {
                when(memberService.updateMember(any(UUID.class), any(Member.UpdateMemberByAdmin.class)))
                        .thenReturn(stubMember());

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
