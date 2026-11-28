package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.GuardiansNotAllowedException;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberDataRequiredException;
import com.klabis.members.domain.MissingDataItem;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST /api/members")
@MembersWebMvcTest
class MemberRegistrationApiTest extends MemberApiTestBase {

    @Autowired
    private RegistrationPort registrationService;

    static ResultMatcher locationHeaderWithMemberDetailRedirect(UUID expectedMemberId) {
        return header().string(HttpHeaders.LOCATION,
                "http://localhost/api/members/%s".formatted(expectedMemberId.toString()));
    }

    private MockHttpServletRequestBuilder postMembers() {
        return post("/api/members")
                .contentType(MediaType.APPLICATION_JSON);
    }

    private static final String VALID_BODY = """
            {
                "firstName": "Jan",
                "lastName": "Novák",
                "dateOfBirth": "2005-05-15",
                "nationality": "CZ",
                "gender": "MALE",
                "email": "jan@example.com",
                "phone": "+420777123456",
                "address": {
                    "street": "Hlavní 123",
                    "city": "Praha",
                    "postalCode": "11000",
                    "country": "CZ"
                }
            }
            """;

    static Stream<Arguments> invalidFields() {
        return Stream.of(
                arguments("firstName", "\"firstName\": \"Jan\"", "\"firstName\": \"\""),
                arguments("email", "\"email\": \"jan@example.com\"", "\"email\": \"invalid-email\""),
                arguments("phone", "\"phone\": \"+420777123456\"", "\"phone\": \"123\""),
                arguments("dateOfBirth", "\"dateOfBirth\": \"2005-05-15\"", "\"dateOfBirth\": \"2120-12-10\""),
                arguments("nationality", "\"nationality\": \"CZ\"", "\"nationality\": \"CZECH\""),
                arguments("address.street", "\"street\": \"Hlavní 123\"", "\"street\": \"\""),
                arguments("address.country", "\"country\": \"CZ\"", "\"country\": \"X\""));
    }

    @Test
    @DisplayName("should return 201 with Location and pass the whole registration to the service")
    @WithKlabisMockUser(username = ADMIN_USERNAME, userId = "729d897a-5676-431f-b22f-ad935af525ce",
            authorities = {Authority.MEMBERS_MANAGE})
    void shouldRegisterMember() throws Exception {
        UUID memberId = UUID.randomUUID();
        when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class)))
                .thenReturn(MemberTestDataBuilder.aMemberWithId(memberId).build());

        mockMvc.perform(postMembers().content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Novák",
                            "dateOfBirth": "2000-01-01",
                            "nationality": "CZ",
                            "gender": "MALE",
                            "email": "jan.novak@example.com",
                            "phone": "+420777123456",
                            "address": {
                                "street": "Hlavní 123",
                                "city": "Praha",
                                "postalCode": "11000",
                                "country": "CZ"
                            },
                            "birthNumber": "0001011234",
                            "bankAccountNumber": "123456789/2010"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(locationHeaderWithMemberDetailRedirect(memberId));

        var commandCaptor = ArgumentCaptor.forClass(RegistrationPort.RegisterNewMember.class);
        verify(registrationService).registerMember(commandCaptor.capture());
        RegistrationPort.RegisterNewMember command = commandCaptor.getValue();
        assertThat(command.personalInformation().getFirstName()).isEqualTo("Jan");
        assertThat(command.personalInformation().getLastName()).isEqualTo("Novák");
        assertThat(command.personalInformation().getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(command.personalInformation().getNationality().code()).isEqualTo("CZ");
        assertThat(command.personalInformation().getGender()).isEqualTo(Gender.MALE);
        assertThat(command.address().street()).isEqualTo("Hlavní 123");
        assertThat(command.address().city()).isEqualTo("Praha");
        assertThat(command.address().postalCode()).isEqualTo("11000");
        assertThat(command.address().country().code()).isEqualTo("CZ");
        assertThat(command.email().value()).isEqualTo("jan.novak@example.com");
        assertThat(command.phone().value()).isEqualTo("+420777123456");
        assertThat(command.birthNumber().value()).isEqualTo("000101/1234");
        assertThat(command.bankAccountNumber().value()).isEqualTo("123456789/2010");
        assertThat(command.registeredBy())
                .isEqualTo(new UserId(UUID.fromString("729d897a-5676-431f-b22f-ad935af525ce")));
    }

    @ParameterizedTest(name = "invalid {0} should return 400 with a field error")
    @MethodSource("invalidFields")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn400ForInvalidField(String fieldKey, String valid, String invalid) throws Exception {
        assertThat(VALID_BODY).contains(valid);

        mockMvc.perform(postMembers().content(VALID_BODY.replace(valid, invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors['%s']".formatted(fieldKey)).exists());

        Mockito.verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("with minor should pass existing and new legal guardians to the service")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldPassLegalGuardiansOfMinor() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID existingGuardian = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
        when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

        mockMvc.perform(postMembers().content("""
                        {
                            "firstName": "Petra",
                            "lastName": "Nováková",
                            "dateOfBirth": "%s",
                            "nationality": "CZ",
                            "gender": "FEMALE",
                            "address": {
                                "street": "Hlavní 456",
                                "city": "Brno",
                                "postalCode": "60000",
                                "country": "CZ"
                            },
                            "legalGuardians": [
                                {"userId": "%s"},
                                {"firstName": "Eva", "lastName": "Nováková",
                                 "email": "eva@example.com", "phone": "+420123456789"}
                            ]
                        }
                        """.formatted(LocalDate.now().minusYears(10), existingGuardian))
                )
                .andExpect(status().isCreated())
                .andExpect(locationHeaderWithMemberDetailRedirect(memberId));

        Mockito.verify(registrationService).registerMember(argThat(cmd ->
                cmd.email() == null && cmd.phone() == null &&
                cmd.takenOverLegalGuardian() == null &&
                cmd.legalGuardians().size() == 2 &&
                existingGuardian.equals(cmd.legalGuardians().get(0).userId().uuid()) &&
                "eva@example.com".equals(cmd.legalGuardians().get(1).newGuardian().email())
        ));
    }

    @Test
    @DisplayName("with legalGuardianUserId should pass the guardian to take over")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldPassTakenOverLegalGuardian() throws Exception {
        UUID memberId = UUID.randomUUID();
        UUID guardian = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
        when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class))).thenReturn(member);

        mockMvc.perform(postMembers().content("""
                        {
                            "firstName": "Eva",
                            "lastName": "Svobodová",
                            "dateOfBirth": "1985-06-20",
                            "nationality": "CZ",
                            "gender": "FEMALE",
                            "email": "eva@example.com",
                            "phone": "+420111222333",
                            "address": {
                                "street": "Hlavní 456",
                                "city": "Brno",
                                "postalCode": "60000",
                                "country": "CZ"
                            },
                            "legalGuardianUserId": "%s"
                        }
                        """.formatted(guardian))
                )
                .andExpect(status().isCreated());

        Mockito.verify(registrationService).registerMember(argThat(cmd ->
                cmd.takenOverLegalGuardian() != null && guardian.equals(cmd.takenOverLegalGuardian().uuid()) &&
                cmd.legalGuardians().isEmpty()
        ));
    }

    @Test
    @DisplayName("with blank email should leave the requirement of own contacts to the service")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldPassBlankEmailAsAbsentToService() throws Exception {
        when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class)))
                .thenThrow(new MemberDataRequiredException(MissingDataItem.EMAIL));

        mockMvc.perform(postMembers().content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Novák",
                            "dateOfBirth": "2005-05-15",
                            "nationality": "CZ",
                            "gender": "MALE",
                            "email": "",
                            "phone": "+420777123456",
                            "address": {
                                "street": "Hlavní 123",
                                "city": "Praha",
                                "postalCode": "11000",
                                "country": "CZ"
                            }
                        }
                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/email-required"))
                .andExpect(jsonPath("$.field").value("email"));

        Mockito.verify(registrationService).registerMember(argThat(cmd -> cmd.email() == null));
    }

    @Test
    @DisplayName("with a malformed birth number should return 400 typed and bound to the birth number field")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn400WithTypeAndFieldWhenBirthNumberMalformed() throws Exception {
        mockMvc.perform(postMembers().content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Novák",
                            "dateOfBirth": "1990-05-15",
                            "nationality": "CZ",
                            "gender": "MALE",
                            "email": "jan@example.com",
                            "phone": "+420777123456",
                            "birthNumber": "12345",
                            "address": {"street": "Hlavní 123", "city": "Praha", "postalCode": "11000", "country": "CZ"}
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/invalid-birth-number"))
                .andExpect(jsonPath("$.field").value("birthNumber"));

        Mockito.verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("a minor with a taken-over guardian should return 400 bound to the legalGuardianUserId field")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn400WhenMinorTakesOverGuardian() throws Exception {
        when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class)))
                .thenThrow(GuardiansNotAllowedException.takeOverByMinor());

        mockMvc.perform(postMembers().content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Novák",
                            "dateOfBirth": "%s",
                            "nationality": "CZ",
                            "gender": "MALE",
                            "legalGuardianUserId": "%s",
                            "address": {"street": "Hlavní 123", "city": "Praha", "postalCode": "11000", "country": "CZ"}
                        }
                        """.formatted(LocalDate.now().minusYears(10), UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/minor-cannot-take-over-guardian"))
                .andExpect(jsonPath("$.field").value("legalGuardianUserId"));
    }

    @Test
    @DisplayName("an adult with guardians should return 400 bound to the legal guardians field")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn400WhenAdultHasGuardians() throws Exception {
        when(registrationService.registerMember(any(RegistrationPort.RegisterNewMember.class)))
                .thenThrow(GuardiansNotAllowedException.adultWithGuardians());

        mockMvc.perform(postMembers().content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Novák",
                            "dateOfBirth": "1990-05-15",
                            "nationality": "CZ",
                            "gender": "MALE",
                            "email": "jan@example.com",
                            "phone": "+420777123456",
                            "address": {"street": "Hlavní 123", "city": "Praha", "postalCode": "11000", "country": "CZ"}
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/adult-member-cannot-have-guardians"))
                .andExpect(jsonPath("$.field").value("legalGuardians"));
    }

    @Test
    @DisplayName("with nationality naming no existing country should return 400 bound to the nationality field and register nobody")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn400WithNationalityFieldWhenNationalityUnknown() throws Exception {
        mockMvc.perform(postMembers().content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Novák",
                            "dateOfBirth": "2005-05-15",
                            "nationality": "QQ",
                            "gender": "MALE",
                            "email": "jan@example.com",
                            "phone": "+420777123456",
                            "address": {
                                "street": "Hlavní 123",
                                "city": "Praha",
                                "postalCode": "11000",
                                "country": "CZ"
                            }
                        }
                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/invalid-nationality"))
                .andExpect(jsonPath("$.field").value("nationality"));

        Mockito.verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("with unknown address country code should return field-level error")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_MANAGE})
    void shouldReturn400WithAddressCountryFieldWhenCountryUnknown() throws Exception {
        mockMvc.perform(postMembers().content("""
                        {
                            "firstName": "Jan",
                            "lastName": "Novák",
                            "dateOfBirth": "2005-05-15",
                            "nationality": "CZ",
                            "gender": "MALE",
                            "email": "jan@example.com",
                            "phone": "+420777123456",
                            "address": {
                                "street": "Hlavní 123",
                                "city": "Praha",
                                "postalCode": "11000",
                                "country": "QQ"
                            }
                        }
                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/errors/invalid-address-country"))
                .andExpect(jsonPath("$.field").value("address.country"));
    }
}
