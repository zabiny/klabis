package com.klabis.members.infrastructure.restapi;

import com.klabis.members.MembersWebMvcTest;
import com.klabis.common.TargetGrant;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.TargetType;
import com.klabis.common.users.Authority;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.domain.*;
import com.klabis.sync.domain.SyncPairing;
import com.klabis.sync.application.SyncPairingLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API tests for PATCH /api/members/{id} endpoint.
 * <p>
 * Tests admin edit, member self-edit, authorization, and validation.
 * <p>
 * <b>Test Scope:</b> Controller unit tests focus on:
 * <ul>
 *   <li>HTTP status codes (204, 400, 403, 401)</li>
 *   <li>Service method invocation with expected command parameters (using Mockito.verify)</li>
 *   <li>Authorization checks for different user roles</li>
 * </ul>
 * <p>
 * The PATCH endpoint returns 204 No Content without response body.
 * To verify updated data, clients should call the GET endpoint after PATCH.
 * <p>
 * <b>What is NOT tested here:</b>
 * <ul>
 *   <li>Request-to-command mapping - tested in {@link UpdateMemberRequestMapperTest}</li>
 *   <li>Domain update logic - tested in domain unit tests</li>
 *   <li>Integration flows - tested in cross-module/E2E tests</li>
 * </ul>
 */
@DisplayName("Update Member Controller API Tests")
@MembersWebMvcTest
class UpdateMemberApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ManagementPort memberService;

    @Autowired
    private SyncPairingLookup pairingLookup;

    @BeforeEach
    void stubPairingNoneByDefault() {
        when(pairingLookup.pairingOf(any())).thenReturn(SyncPairing.NONE);
    }

    private final UUID testMemberId = UUID.randomUUID();

    private Member stubMember() {
        return MemberTestDataBuilder.aMember()
                .withId(testMemberId)
                .withFirstName("Jan")
                .withLastName("Novak")
                .withRegistrationNumber("ZBM1234")
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withEmail("jan.novak@example.com")
                .withPhone("+420777123456")
                .withAddress(Address.of("Hlavní 1", "Praha", "11000", Country.of("CZ")))
                .build();
    }

    private static final String OWNER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String STRANGER_ID = "16a9dfbb-145d-4587-b5b5-31383c1d4215";
    private static final String ADMIN_ID = "729d897a-5676-431f-b22f-ad935af525ce";

    private void givenBaseline() {
        when(memberService.prefilledUpdateCommand(any(MemberId.class)))
                .thenReturn(Member.UpdateMember.from(stubMember()));
        when(memberService.updateMember(any(MemberId.class), any(Member.UpdateMember.class)))
                .thenReturn(stubMember());
    }

    private org.springframework.test.web.servlet.ResultActions patchJson(UUID id, String json) throws Exception {
        return mockMvc.perform(patch("/api/members/{id}", id).contentType("application/json").content(json));
    }

    @Nested
    @DisplayName("PATCH /api/members/{id}")
    class UpdateMemberTests {

        @Test
        @DisplayName("administrator changes contact and reserved fields: 204, command is the baseline overlaid by the request and carries the editor")
        @WithKlabisMockUser(userId = ADMIN_ID, authorities = {Authority.MEMBERS_MANAGE})
        void shouldUpdateContactAndReservedFieldsWhenAdmin() throws Exception {
            givenBaseline();

            patchJson(testMemberId, """
                    {"email": "new.email@example.com", "firstName": "Petr", "gender": "FEMALE",
                     "drivingLicenseGroup": "B",
                     "trainerLicense": {"level": "T2", "validityDate": "2099-01-01"}}
                    """)
                    .andExpect(status().isNoContent());

            var captor = forClass(Member.UpdateMember.class);
            verify(memberService).updateMember(any(MemberId.class), captor.capture());
            var command = captor.getValue();
            assertThat(command.email()).isEqualTo(EmailAddress.of("new.email@example.com"));
            assertThat(command.firstName()).isEqualTo("Petr");
            assertThat(command.gender()).isEqualTo(com.klabis.members.domain.Gender.FEMALE);
            assertThat(command.drivingLicenseGroup()).isEqualTo(com.klabis.members.domain.DrivingLicenseGroup.B);
            assertThat(command.trainerLicense().level()).isEqualTo(TrainerLevel.T2);
            assertThat(command.phone()).isEqualTo(PhoneNumber.of("+420777123456"));
            assertThat(command.updatedBy()).isEqualTo(new UserId(UUID.fromString(ADMIN_ID)));
        }

        @Test
        @DisplayName("member updates own email: 204")
        @WithKlabisMockUser(memberId = OWNER_ID, authorities = {},
                targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                        ids = OWNER_ID))
        void shouldAllowMemberToUpdateOwnEmail() throws Exception {
            UUID currentMemberId = UUID.fromString(OWNER_ID);
            givenBaseline();

            patchJson(currentMemberId, "{\"email\": \"my.new.email@example.com\"}")
                    .andExpect(status().isNoContent());

            var captor = forClass(Member.UpdateMember.class);
            verify(memberService).updateMember(eq(new MemberId(currentMemberId)), captor.capture());
            assertThat(captor.getValue().email()).isEqualTo(EmailAddress.of("my.new.email@example.com"));
        }

        /**
         * The owner is the only caller for whom field-level authorization decides anything: a
         * stranger is already refused by the endpoint's @HasAuthority, and an admin satisfies
         * every field.
         *
         * <p>gender is the field this guards because it reached production unenforced: an
         * allOf in the spec generated a bare Gender rather than a JsonNullable, and
         * RequestBodyFieldAuthorizationAdvice skips every component that is not
         * JsonNullable-typed.
         */
        @ParameterizedTest(name = "own {0} is refused with 403")
        @CsvSource({
                "firstName,Karel",
                "lastName,Changed",
                "dateOfBirth,1991-02-03",
                "gender,FEMALE",
                "nationality,SK",
                "birthNumber,900101/1234"
        })
        @WithKlabisMockUser(memberId = OWNER_ID, authorities = {},
                targetGrants = @TargetGrant(authority = Authority.MEMBERS_EDIT_PROFILE, type = TargetType.MEMBER,
                        ids = OWNER_ID))
        void shouldRejectMemberUpdatingOwnReservedField(String field, String value) throws Exception {
            givenBaseline();

            patchJson(UUID.fromString(OWNER_ID), "{\"%s\": \"%s\"}".formatted(field, value))
                    .andExpect(status().isForbidden());

            verify(memberService, never()).updateMember(any(MemberId.class), any(Member.UpdateMember.class));
        }

        @Test
        @DisplayName("caller who is neither the member, a holder of the delegated permission nor an administrator gets 403 for any field")
        @WithKlabisMockUser(userId = STRANGER_ID,
                memberId = "22222222-2222-2222-2222-222222222222", authorities = {})
        void shouldReturn403WhenStrangerEditsMember() throws Exception {
            givenBaseline();

            patchJson(testMemberId, """
                    {"email": "hacker@example.com", "dietaryRestrictions": "none"}
                    """)
                    .andExpect(status().isForbidden());

            verify(memberService, never()).updateMember(any(MemberId.class), any(Member.UpdateMember.class));
        }

        @Test
        @DisplayName("unauthenticated request should return 401")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            patchJson(testMemberId, "{\"email\": \"test@example.com\"}")
                    .andExpect(status().isUnauthorized());
        }

        @ParameterizedTest(name = "{0} is refused at its own field with its own type")
        @CsvSource({
                "email,invalid-email,invalid-email-address",
                "phone,123,invalid-phone-number",
                "birthNumber,12345,invalid-birth-number",
                "bankAccountNumber,abc,invalid-bank-account-number",
                "nationality,QQ,invalid-nationality"
        })
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WithTypeAndFieldWhenValueInvalid(String field, String value, String type) throws Exception {
            givenBaseline();

            patchJson(testMemberId, "{\"%s\": \"%s\"}".formatted(field, value))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/" + type))
                    .andExpect(jsonPath("$.field").value(field))
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("address with an unknown country is refused at address.country and nothing is saved")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WithAddressCountryFieldWhenCountryUnknown() throws Exception {
            givenBaseline();

            patchJson(testMemberId, """
                    {"address": {"street": "Hlavní 1", "city": "Praha", "postalCode": "11000", "country": "QQ"}}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/errors/invalid-address-country"))
                    .andExpect(jsonPath("$.field").value("address.country"));

            verify(memberService, never()).updateMember(any(MemberId.class), any(Member.UpdateMember.class));
        }

        static Stream<Arguments> lengthAndFormatViolations() {
            return Stream.of(
                    violation("chipNumber with letters", "{\"chipNumber\": \"ABC123\"}"),
                    violation("chipNumber over 50 characters", "{\"chipNumber\": \"%s\"}".formatted("1".repeat(51))),
                    violation("dietaryRestrictions over 500 characters",
                            "{\"dietaryRestrictions\": \"%s\"}".formatted("A".repeat(501))),
                    violation("firstName over 100 characters", "{\"firstName\": \"%s\"}".formatted("A".repeat(101))),
                    violation("lastName over 100 characters", "{\"lastName\": \"%s\"}".formatted("A".repeat(101))),
                    violation("email over 255 characters",
                            "{\"email\": \"%s\"}".formatted("a".repeat(244) + "@example.com")),
                    violation("phone over 50 characters", "{\"phone\": \"%s\"}".formatted("+420" + "1".repeat(47))),
                    violation("nationality over 2 characters", "{\"nationality\": \"CZEE\"}"),
                    violation("bankAccountNumber over 50 characters",
                            "{\"bankAccountNumber\": \"%s\"}".formatted("1".repeat(51))),
                    violation("identityCard number over 50 characters",
                            "{\"identityCard\": {\"cardNumber\": \"%s\", \"validityDate\": \"2030-01-01\"}}"
                                    .formatted("A".repeat(51))));
        }

        private static Arguments violation(String name, String json) {
            return Arguments.of(Named.of(name, json));
        }

        @ParameterizedTest(name = "{0} should return 400 not 500")
        @MethodSource("lengthAndFormatViolations")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_MANAGE})
        void shouldReturn400WhenFieldViolatesLengthOrFormat(String json) throws Exception {
            patchJson(testMemberId, json)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }
    }
}
