package com.klabis.members.infrastructure.restapi;

import com.klabis.common.HateoasTestingSupport;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserService;
import com.klabis.members.MemberId;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.application.InvalidUpdateException;
import com.klabis.members.application.ManagementService;
import com.klabis.members.application.RegistrationService;
import com.klabis.members.application.ValidationPatterns;
import com.klabis.members.domain.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for MemberController.
 * <p>
 * Tests validation, error handling, and HATEOAS link generation.
 * <p>
 * <b>Test Scope:</b> Controller unit tests focus on:
 * <ul>
 *   <li>HTTP status codes (200, 201, 204, 400, 403, 404, etc.)</li>
 *   <li>Service method invocation with expected parameters (using Mockito.verify)</li>
 *   <li>HATEOAS links presence and basic structure</li>
 *   <li>Exception handling and error responses</li>
 * </ul>
 * <p>
 * <b>What is NOT tested here:</b>
 * <ul>
 *   <li>Detailed JSON field-by-field mapping - tested in {@link MemberMappingTests}</li>
 *   <li>Domain logic - tested in domain unit tests</li>
 *   <li>Integration flows - tested in integration/E2E tests</li>
 * </ul>
 */
@DisplayName("Member Controller API Tests")
@WebMvcTest(controllers = {MemberController.class, RegistrationController.class})
@Import({MemberMapperImpl.class})
@MockitoBean(types = {UserService.class, UserDetailsService.class})
class MemberControllerApiTest {

    private static final String ADMIN_USERNAME = "ZBM0001";
    private static final String MEMBER_USERNAME = "ZBM0101";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagementService managementService;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private RegistrationService registrationService;

    @TestBean
    private EntityLinks entityLinks;

    static EntityLinks entityLinks() {
        return HateoasTestingSupport.createModuleEntityLinks(MemberController.class);
    }

    @Nested
    @DisplayName("GET /api/members/{id}")
    class GetMemberTests {

        private MockHttpServletRequestBuilder getMemberById(MemberId memberId) {
            return getMemberById(memberId.uuid());
        }

        private MockHttpServletRequestBuilder getMemberById(UUID memberId) {
            return get("/api/members/{id}", memberId.toString())
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE);
        }

        @Test
        @DisplayName("should return 200 with member details")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        void shouldReturnMemberDetailsWhenFound() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withFirstName("Jan")
                    .withLastName("Novák")
                    .withRegistrationNumber("ZBM0501")
                    .withEmail("jan.novak@example.com")
                    .withDateOfBirth(LocalDate.of(2005, 6, 15))
                    .withGender(Gender.MALE)
                    .withPhone("+420777888999")
                    .withAddress(new Address("Hlavní 123", "Praha", "11000", "CZ"))
                    .withActive(true)
                    .withNationality("CZ")
                    .build();

            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(getMemberById(member.getId()))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaTypes.HAL_FORMS_JSON_VALUE))
                    // Assert only key fields - detailed JSON structure is tested in MemberMappingTests
                    .andExpect(jsonPath("$.id").value(memberId.toString()))
                    .andExpect(jsonPath("$.registrationNumber").value("ZBM0501"))
                    .andExpect(jsonPath("$.firstName").exists())
                    .andExpect(jsonPath("$.lastName").exists())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.phone").exists())
                    .andExpect(jsonPath("$.address").exists())
                    .andExpect(jsonPath("$.active").value(true))
                    // Assert HATEOAS links presence
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString(
                            "/api/members/" + memberId)))
                    .andExpect(jsonPath("$._links.collection.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").value(org.hamcrest.Matchers.containsString(
                            "/api/members")));
        }

        @Test
        @DisplayName("should return 404 when member not found")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        void shouldReturn404WhenMemberNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.empty());

            mockMvc.perform(getMemberById(nonExistentId))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                            nonExistentId.toString())));
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:READ authority")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_CREATE})
        void shouldReturn403WhenUnauthorized() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return guardian information when present")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        void shouldReturnGuardianInformationWhenPresent() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withGuardian(new GuardianInformation("Parent",
                            "Name",
                            "PARENT",
                            "parent@example.com",
                            "+420777111222"))
                    .build();

            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    // Assert only guardian presence - detailed structure is tested in MemberMappingTests
                    .andExpect(jsonPath("$.guardian").isNotEmpty())
                    .andExpect(jsonPath("$.guardian.firstName").exists())
                    .andExpect(jsonPath("$.guardian.lastName").exists())
                    .andExpect(jsonPath("$.guardian.relationship").exists())
                    .andExpect(jsonPath("$.guardian.email").exists())
                    .andExpect(jsonPath("$.guardian.phone").exists());
        }

        @Test
        @DisplayName("should return single email and phone with address")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        void shouldReturnSingleEmailAndPhoneWithAddress() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withAddress(new Address("Main Street 123", "Bratislava", "81101", "SK"))
                    .build();

            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    // Assert only address presence - detailed structure is tested in MemberMappingTests
                    .andExpect(jsonPath("$.address").isNotEmpty())
                    .andExpect(jsonPath("$.address.street").exists())
                    .andExpect(jsonPath("$.address.city").exists())
                    .andExpect(jsonPath("$.address.postalCode").exists())
                    .andExpect(jsonPath("$.address.country").exists());
        }

        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS_READ permission - should include only update affordance (no permissions neither terminate)")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ})
        @Disabled("need to finish authorization for klabisAfford")
        void shouldNotIncludePermissionsLinkWhenUserLacksMembersPermissionsAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            Address address = Address.of("Test Street", "Test City", "10000", "CZ");
            EmailAddress email = EmailAddress.of("test@example.com");
            PhoneNumber phone = PhoneNumber.of("+420123456789");

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withName("Test", "Member")
                    .withRegistrationNumber("ZBM1234")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withPhone(phone)
                    .withEmail(email)
                    .build();

            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.permissions").doesNotExist())
                    .andExpect(jsonPath("$._templates").exists())
                    .andExpect(jsonPath("$._templates.default.method").value("PATCH"))  // "UPDATE member"
                    .andExpect(jsonPath("$._templates.default.target").doesNotExist())
                    .andExpect(jsonPath("$._templates.suspendMember").doesNotExist());
        }

        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS_PERMISSIONS authority: should include permissions link")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_PERMISSIONS})
        void activeMemberShouldReturnPermissionsLink() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .build();

            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(getMemberById(memberId))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$._links.permissions.href").value("http://localhost/api/users/" + memberId + "/permissions"));
        }

        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS_PERMISSIONS authority: should not return permissions link for deactivated member response")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_PERMISSIONS})
        void activeMemberShouldNotReturnPermissionsLinkForDeactivatedMember() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(false)
                    .build();

            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(getMemberById(memberId))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$._links.permissions").doesNotExist());
        }


        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS_UPDATE authority: should include update and suspend in active member response (no permissions)")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_UPDATE})
        void activeMemberShouldReturnUpdateAndSuspendAffordances() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(true)
                    .build();

            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$._templates").exists())
                    .andExpect(jsonPath("$._templates.default.method").value("PATCH"))  // "UPDATE member"
                    .andExpect(jsonPath("$._templates.default.target").doesNotExist())
                    .andExpect(jsonPath("$._templates.suspendMember.method").value("POST"))
                    .andExpect(jsonPath("$._templates.suspendMember.target").value(
                            "http://localhost/api/members/%s/suspend".formatted(memberId)));
        }

        @Test
        @DisplayName("HAL+FORMS: user with MEMBERS_UPDATE authority: should include update and resume affordances for suspended member")
        @WithKlabisMockUser(username = "ZBM0001", authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_UPDATE})
        void suspendedMemberShouldReturnUpdateAndResumeAffordances() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withActive(false)
                    .build();

            when(memberRepository.findById(any(MemberId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(getMemberById(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$._links.permissions").doesNotExist())
                    .andExpect(jsonPath("$._templates").exists())
                    .andExpect(jsonPath("$._templates.default.method").value("PATCH"))
                    .andExpect(jsonPath("$._templates.default.target").doesNotExist())
                    .andExpect(jsonPath("$._templates.suspendMember").doesNotExist())
                    .andExpect(jsonPath("$._templates.resumeMember.method").value("POST"))
                    .andExpect(jsonPath("$._templates.resumeMember.target").value(
                            "http://localhost/api/members/%s/resume".formatted(memberId)));
        }
    }

    @Nested
    @DisplayName("POST /api/members")
    class RegisterMemberTests {

        static ResultMatcher locationHeaderWithMemberDetailRedirect(UUID expectedMemberId) {
            return header().string(HttpHeaders.LOCATION,
                    "http://localhost/api/members/%s".formatted(expectedMemberId.toString()));
        }

        private MockHttpServletRequestBuilder postMembers() {
            return post("/api/members")
                    .contentType(MediaType.APPLICATION_JSON);
        }

        @Test
        @DisplayName("should call service with correct personal information arguments")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {com.klabis.common.users.Authority.MEMBERS_CREATE})
        void shouldCallServiceWithCorrectPersonalInformation() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationService.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                    {
                        "firstName": "Jan",
                        "lastName": "Novák",
                        "dateOfBirth": "2000-06-15",
                        "nationality": "CZ",
                        "gender": "MALE",
                        "email": "jan.novak@example.com",
                        "phone": "+420777123456",
                        "address": {
                            "street": "Hlavní 123",
                            "city": "Praha",
                            "postalCode": "11000",
                            "country": "CZ"
                        }
                    }
                    """));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.personalInformation().getFirstName().equals("Jan") &&
                    cmd.personalInformation().getLastName().equals("Novák") &&
                    cmd.personalInformation().getDateOfBirth().equals(LocalDate.of(2000, 6, 15)) &&
                    cmd.personalInformation().getNationalityCode().equals("CZ") &&
                    cmd.personalInformation().getGender() == Gender.MALE
            ));
        }

        @Test
        @DisplayName("should call service with correct address arguments")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {com.klabis.common.users.Authority.MEMBERS_CREATE})
        void shouldCallServiceWithCorrectAddress() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationService.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                    {
                        "firstName": "Jan",
                        "lastName": "Novák",
                        "dateOfBirth": "2000-06-15",
                        "nationality": "CZ",
                        "gender": "MALE",
                        "email": "jan.novak@example.com",
                        "phone": "+420777123456",
                        "address": {
                            "street": "Hlavní 123",
                            "city": "Praha",
                            "postalCode": "11000",
                            "country": "CZ"
                        }
                    }
                    """));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.address() != null &&
                    cmd.address().street().equals("Hlavní 123") &&
                    cmd.address().city().equals("Praha") &&
                    cmd.address().postalCode().equals("11000") &&
                    cmd.address().country().equals("CZ")
            ));
        }

        @Test
        @DisplayName("should call service with correct email and phone arguments")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {com.klabis.common.users.Authority.MEMBERS_CREATE})
        void shouldCallServiceWithCorrectEmailAndPhone() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationService.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                    {
                        "firstName": "Jan",
                        "lastName": "Novák",
                        "dateOfBirth": "2000-06-15",
                        "nationality": "CZ",
                        "gender": "MALE",
                        "email": "jan.novak@example.com",
                        "phone": "+420777123456",
                        "address": {
                            "street": "Hlavní 123",
                            "city": "Praha",
                            "postalCode": "11000",
                            "country": "CZ"
                        }
                    }
                    """));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.email().value().equals("jan.novak@example.com") &&
                    cmd.phone().value().equals("+420777123456")
            ));
        }

        @Test
        @DisplayName("should call service with correct guardian arguments for minor")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldCallServiceWithCorrectGuardian() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationService.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                    {
                        "firstName": "Petra",
                        "lastName": "Nováková",
                        "dateOfBirth": "2010-06-20",
                        "nationality": "CZ",
                        "gender": "FEMALE",
                        "email": "petra.novakova@example.com",
                        "phone": "+420111222333",
                        "address": {
                            "street": "Hlavní 456",
                            "city": "Brno",
                            "postalCode": "60000",
                            "country": "CZ"
                        },
                        "guardian": {
                            "firstName": "Guardian",
                            "lastName": "Surname",
                            "relationship": "PARENT",
                            "email": "guardian@example.com",
                            "phone": "+420123456789"
                        }
                    }
                    """));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.guardian() != null &&
                    cmd.guardian().getFirstName().equals("Guardian") &&
                    cmd.guardian().getLastName().equals("Surname") &&
                    cmd.guardian().getRelationship().equals("PARENT") &&
                    cmd.guardian().getEmailValue().equals("guardian@example.com") &&
                    cmd.guardian().getPhoneValue().equals("+420123456789")
            ));
        }

        @Test
        @DisplayName("should call service with null guardian when not provided")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldCallServiceWithNullGuardianWhenNotProvided() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationService.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                    {
                        "firstName": "Jan",
                        "lastName": "Novák",
                        "dateOfBirth": "2000-06-15",
                        "nationality": "CZ",
                        "gender": "MALE",
                        "email": "jan.novak@example.com",
                        "phone": "+420777123456",
                        "address": {
                            "street": "Hlavní 123",
                            "city": "Praha",
                            "postalCode": "11000",
                            "country": "CZ"
                        }
                    }
                    """));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.guardian() == null
            ));
        }

        @Test
        @DisplayName("should call service with correct birthNumber and bankAccountNumber when provided")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {com.klabis.common.users.Authority.MEMBERS_CREATE})
        void shouldCallServiceWithCorrectBirthNumberAndBankAccount() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId).build();
            when(registrationService.registerMember(any(RegistrationService.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                    {
                        "firstName": "Jan",
                        "lastName": "Novák",
                        "dateOfBirth": "2000-06-15",
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
                    """));

            Mockito.verify(registrationService).registerMember(argThat(cmd ->
                    cmd.birthNumber() != null &&
                    cmd.birthNumber().value().equals("000101/1234") &&
                    cmd.bankAccountNumber() != null &&
                    cmd.bankAccountNumber().value().equals("123456789/2010")
            ));
        }

        @Test
        @DisplayName("with valid data should return 201 with HATEOAS links")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {com.klabis.common.users.Authority.MEMBERS_CREATE})
        void shouldCreateMemberWithValidData() throws Exception {
            UUID memberId = UUID.randomUUID();

            Address address = Address.of("Test Street", "Test City", "10000", "CZ");
            EmailAddress email = EmailAddress.of("test@example.com");
            PhoneNumber phone = PhoneNumber.of("+420123456789");

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withName("Test", "Member")
                    .withRegistrationNumber("ZBM1234")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withPhone(phone)
                    .withEmail(email)
                    .build();

            when(registrationService.registerMember(any(RegistrationService.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(
                            postMembers().content("""
                                    {
                                        "firstName": "Jan",
                                        "lastName": "Novák",
                                        "dateOfBirth": "2000-06-15",
                                        "nationality": "CZ",
                                        "gender": "MALE",
                                        "email": "jan.novak@example.com",
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
                    .andExpect(status().isCreated())
                    .andExpect(locationHeaderWithMemberDetailRedirect(memberId));
        }

        @Test
        @DisplayName("with minor should accept guardian")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldCreateMinorWithGuardian() throws Exception {
            UUID memberId = UUID.randomUUID();

            Address address = Address.of("Test Street", "Test City", "10000", "CZ");
            EmailAddress email = EmailAddress.of("test@example.com");
            PhoneNumber phone = PhoneNumber.of("+420123456789");

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withName("Test", "Member")
                    .withRegistrationNumber("ZBM1234")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withPhone(phone)
                    .withEmail(email)
                    .build();

            when(registrationService.registerMember(any(RegistrationService.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Petra",
                                "lastName": "Nováková",
                                "dateOfBirth": "2010-06-20",
                                "nationality": "CZ",
                                "gender": "FEMALE",
                                "email": "petra.novakova@example.com",
                                "phone": "+420111222333",
                                "address": {
                                    "street": "Hlavní 456",
                                    "city": "Brno",
                                    "postalCode": "60000",
                                    "country": "CZ"
                                },
                                "guardian": {
                                    "firstName": "Guardian",
                                    "lastName": "Surname",
                                    "relationship": "PARENT",
                                    "email": "guardian@example.com",
                                    "phone": "+420123456789"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isCreated())
                    .andExpect(locationHeaderWithMemberDetailRedirect(memberId));
        }

        @Test
        @DisplayName("with missing first name should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldReturn400WhenFirstNameMissing() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "",
                                "lastName": "Novák",
                                "dateOfBirth": "2000-06-15",
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
                            """)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.firstName").value("First name is required"));
        }

        @Test
        @DisplayName("with invalid email should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldReturn400WhenEmailInvalid() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "invalid-email",
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
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.email").value("Email must be valid"));
        }

        @Test
        @DisplayName("with invalid phone should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldReturn400WhenPhoneInvalid() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "jan@example.com",
                                "phone": "123",
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
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.phone").value(
                            "Phone number must be in E.164 format (starts with +)"));
        }

        @Test
        @DisplayName("with future date of birth should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldReturn400WhenDateOfBirthInFuture() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2120-12-10",
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
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.dateOfBirth").value("Date of birth must be in the past"));
        }

        @Test
        @DisplayName("with missing email should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldReturn400WhenEmailMissing() throws Exception {
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
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.email").value("Email is required"));
        }

        @Test
        @DisplayName("with invalid guardian should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldReturn400WhenGuardianInvalid() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Petra",
                                "lastName": "Nováková",
                                "dateOfBirth": "2010-06-20",
                                "nationality": "CZ",
                                "gender": "FEMALE",
                                "email": "petra@example.com",
                                "phone": "+420111222333",
                                "address": {
                                    "street": "Hlavní 456",
                                    "city": "Brno",
                                    "postalCode": "60000",
                                    "country": "CZ"
                                },
                                "guardian": {
                                    "firstName": "",
                                    "lastName": "Novák",
                                    "relationship": "PARENT",
                                    "email": "pavel.novak@example.com",
                                    "phone": "+420987654321"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors['guardian.firstName']").value("Guardian first name is required"));
        }

        @Test
        @DisplayName("with valid address and contacts should succeed")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldCreateMemberWithValidAddressAndContacts() throws Exception {
            UUID memberId = UUID.randomUUID();

            Address address = Address.of("Test Street", "Test City", "10000", "CZ");
            EmailAddress email = EmailAddress.of("test@example.com");
            PhoneNumber phone = PhoneNumber.of("+420123456789");

            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withName("Test", "Member")
                    .withRegistrationNumber("ZBM1234")
                    .withDateOfBirth(LocalDate.of(2000, 1, 1))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(address)
                    .withPhone(phone)
                    .withEmail(email)
                    .build();

            when(registrationService.registerMember(any(RegistrationService.RegisterNewMember.class))).thenReturn(member);

            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZ",
                                "gender": "MALE",
                                "email": "jan.novak@example.com",
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
                    .andExpect(status().isCreated())
                    .andExpect(locationHeaderWithMemberDetailRedirect(memberId));
        }

        @Test
        @DisplayName("with invalid nationality code should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldReturn400WhenNationalityCodeInvalid() throws Exception {
            mockMvc.perform(postMembers().content("""
                            {
                                "firstName": "Jan",
                                "lastName": "Novák",
                                "dateOfBirth": "2005-05-15",
                                "nationality": "CZECH",
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
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.nationality").value(
                            "Nationality must be 2 or 3 characters (ISO code)"));
        }

        @Test
        @DisplayName("with invalid address missing fields should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldReturn400WhenAddressMissingFields() throws Exception {
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
                                    "street": "",
                                    "city": "Praha",
                                    "postalCode": "11000",
                                    "country": "CZ"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors['address.street']").value("Street is required"));
        }

        @Test
        @DisplayName("with invalid address country code format should return 400")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_CREATE})
        void shouldReturn400WhenAddressCountryInvalid() throws Exception {
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
                                    "country": "X"
                                }
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors['address.country']").value(ValidationPatterns.MESSAGE_COUNTRY_INVALID));
        }
    }

    @Nested
    @DisplayName("GET /api/members")
    class ListMembersTests {
        private MockHttpServletRequestBuilder getApiMembers() {
            return get("/api/members")
                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE);
        }

        @Test
        @DisplayName("should return 200 with empty collection when no members exist")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturnEmptyCollectionWhenNoMembers() throws Exception {
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(
                    List.of()));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList").doesNotExist())
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("should call repository with correct default pagination parameters")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldCallRepositoryWithDefaultPagination() throws Exception {
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers());

            Mockito.verify(memberRepository).findAll(argThat(pageable ->
                    pageable.getPageNumber() == 0 &&
                    pageable.getPageSize() == 10 &&
                    pageable.getSort().getOrderFor("lastName") != null &&
                    pageable.getSort().getOrderFor("lastName").isAscending()
            ));
        }

        @Test
        @DisplayName("should call repository with custom page and size parameters")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldCallRepositoryWithCustomPagination() throws Exception {
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers()
                    .param("page", "2")
                    .param("size", "20"));

            Mockito.verify(memberRepository).findAll(argThat(pageable ->
                    pageable.getPageNumber() == 2 &&
                    pageable.getPageSize() == 20
            ));
        }

        @Test
        @DisplayName("should call repository with correct sort parameters")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldCallRepositoryWithCorrectSortParameters() throws Exception {
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(getApiMembers()
                    .param("sort", "firstName,desc")
                    .param("sort", "registrationNumber,asc"));

            Mockito.verify(memberRepository).findAll(argThat(pageable ->
                    pageable.getSort().getOrderFor("firstName") != null &&
                    pageable.getSort().getOrderFor("firstName").isDescending() &&
                    pageable.getSort().getOrderFor("registrationNumber") != null &&
                    pageable.getSort().getOrderFor("registrationNumber").isAscending()
            ));
        }

        @Test
        @DisplayName("should return correct JSON structure with page metadata")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturnCorrectJsonStructureWithPageMetadata() throws Exception {
            UUID memberId1 = UUID.randomUUID();
            UUID memberId2 = UUID.randomUUID();
            Member member1 = MemberTestDataBuilder.aMemberWithId(memberId1)
                    .withFirstName("Jan")
                    .withLastName("Novák")
                    .withRegistrationNumber(RegistrationNumber.of("ZBM0001"))
                    .build();
            Member member2 = MemberTestDataBuilder.aMemberWithId(memberId2)
                    .withFirstName("Petra")
                    .withLastName("Svobodová")
                    .withRegistrationNumber(RegistrationNumber.of("ZBM0002"))
                    .build();

            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member1, member2), PageRequest.of(0, 10), 2));

            mockMvc.perform(getApiMembers())
                    .andExpect(status().isOk())
                    // Assert only basic structure - detailed member field mapping is tested in MemberMappingTests
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList").isArray())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList.length()").value(2))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0].id").value(memberId1.toString()))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._links.self.href").exists())
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[1].id").value(memberId2.toString()))
                    .andExpect(jsonPath("$._embedded.memberSummaryResponseList[1]._links.self.href").exists())
                    // Assert page metadata
                    .andExpect(jsonPath("$.page.size").value(10))
                    .andExpect(jsonPath("$.page.totalElements").value(2))
                    .andExpect(jsonPath("$.page.totalPages").value(1))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("should return 400 when sort field invalid")
        @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturn400WhenSortFieldInvalid() throws Exception {
            mockMvc.perform(getApiMembers()
                            .param("sort", "invalidField,asc")
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                            "Invalid sort field")));
        }
    }

    @Nested
    @DisplayName("POST /api/members/{id}/suspend")
    class SuspendMemberTests {

        private MockHttpServletRequestBuilder postMemberIdSuspend(MemberId memberId) {
            return postMemberIdSuspend(memberId.uuid());
        }

        private MockHttpServletRequestBuilder postMemberIdSuspend(UUID memberId) {
            return post("/api/members/" + memberId.toString() + "/suspend")
                    .contentType("application/json");
        }

        @Test
        @DisplayName("it should call expected service method with correct arguments")
        @WithKlabisMockUser(userId = "48e11797-a61b-4783-bc1d-1c11d1b1d288", authorities = {Authority.MEMBERS_UPDATE})
        void shouldCallExpectedServiceMethodWithCorrectArguments() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();

            // Act
            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                    {
                        "reason": "ODHLASKA",
                        "note": "Member requested termination"
                    }
                    """)
            );

            // Assert
            Member.SuspendMembership expectedCommand = new Member.SuspendMembership(UserId.fromString(
                    "48e11797-a61b-4783-bc1d-1c11d1b1d288"),
                    DeactivationReason.ODHLASKA,
                    "Member requested termination");

            Mockito.verify(managementService)
                    .suspendMember(eq(new MemberId(memberId)), eq(expectedCommand));
        }

        @Test
        @DisplayName("valid suspension request should return 204 NO CONTENT response")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
        void shouldSuspendMemberSuccessfully() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                            {
                                "reason": "ODHLASKA",
                                "note": "Member requested termination"
                            }
                            """)
                    )
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/members"));
        }

        @Test
        @DisplayName("should return 400 Bad Request when service throws InvalidUpdateException")
        @WithKlabisMockUser(authorities = {Authority.MEMBERS_UPDATE})
        void shouldReturn400WhenSuspendingAlreadySuspendedMember() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();

            when(managementService.suspendMember(eq(new MemberId(memberId)),
                    any(Member.SuspendMembership.class)))
                    .thenThrow(new InvalidUpdateException("Member is already suspended"));

            // Act & Assert
            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                            {
                                "reason": "OTHER",
                                "note": "Second termination attempt"
                            }
                            """)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("already suspended")));
        }

        @Test
        @DisplayName("suspension with invalid reason should return 400 Bad Request")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_UPDATE})
        void shouldReturn400WhenSuspensionReasonInvalid() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();

            // Act & Assert
            mockMvc.perform(postMemberIdSuspend(memberId).content("""
                            {
                                "reason": null,
                                "note": "Test note"
                            }
                            """)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/members/{id}/resume")
    class ResumeMemberTests {

        private MockHttpServletRequestBuilder postMemberIdResume(UUID memberId) {
            return post("/api/members/" + memberId.toString() + "/resume");
        }

        @Test
        @DisplayName("valid resume request should return 204 NO CONTENT response")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_UPDATE})
        void shouldResumeMemberSuccessfully() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(postMemberIdResume(memberId))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNoContent())
                    .andExpect(header().exists(HttpHeaders.LOCATION));
        }

        @Test
        @DisplayName("should call service with correct arguments")
        @WithKlabisMockUser(userId = "48e11797-a61b-4783-bc1d-1c11d1b1d288", authorities = {Authority.MEMBERS_UPDATE})
        void shouldCallServiceWithCorrectArguments() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(postMemberIdResume(memberId));

            Member.ResumeMembership expectedCommand = new Member.ResumeMembership(
                    UserId.fromString("48e11797-a61b-4783-bc1d-1c11d1b1d288"));
            Mockito.verify(managementService).resumeMember(eq(new MemberId(memberId)), eq(expectedCommand));
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:UPDATE authority")
        @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
        void shouldReturn403WhenUnauthorized() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(postMemberIdResume(memberId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(postMemberIdResume(memberId))
                    .andExpect(status().isUnauthorized());
        }
    }
}
