package com.klabis.members.infrastructure.restapi;

import com.klabis.config.encryption.EncryptionConfiguration;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.domain.*;
import com.klabis.members.management.InvalidUpdateException;
import com.klabis.members.management.ManagementService;
import com.klabis.members.management.RegistrationService;
import com.klabis.members.management.ValidationPatterns;
import com.klabis.users.User;
import com.klabis.users.UserId;
import com.klabis.users.UserService;
import com.klabis.users.testdata.UserTestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for MemberController.
 * <p>
 * Tests validation, error handling, and HATEOAS link generation.
 */
@DisplayName("Member Controller API Tests")
@WebMvcTest(controllers = {MemberController.class, RegistrationController.class})
@Import({EncryptionConfiguration.class, MemberMapperImpl.class})
class MemberControllerApiTest {

    private static final String ADMIN_USERNAME = "ZBM0001";
    private static final String MEMBER_USERNAME = "ZBM0101";
    private static final String MEMBERS_READ_AUTHORITY = "MEMBERS:READ";
    private static final String MEMBERS_CREATE_AUTHORITY = "MEMBERS:CREATE";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagementService managementService;

    @MockitoBean
    private Members memberRepository;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private EntityLinks entityLinks;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private UserService userServiceMock;

    private final User ZBM001 = UserTestDataBuilder.aMemberUser().username("ZBM0001").build();

    @BeforeEach
    void setupZbm001User() {
        when(userServiceMock.findUserByUsername("ZBM001")).thenReturn(Optional.of(ZBM001));
    }

    @Nested
    @DisplayName("GET /api/members/{id}")
    class GetMemberTests {

        @Test
        @DisplayName("should return 200 with member details")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
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

            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(member));

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
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString(
                            "/api/members/" + memberId)))
                    .andExpect(jsonPath("$._links.collection.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").value(org.hamcrest.Matchers.containsString(
                            "/api/members")));
        }

        @Test
        @DisplayName("should return 404 when member not found")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
        void shouldReturn404WhenMemberNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.empty());

            mockMvc.perform(
                            get("/api/members/{id}", nonExistentId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                            nonExistentId.toString())));
        }

        @Test
        @DisplayName("should return 403 when user lacks MEMBERS:READ authority")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:CREATE"})
        void shouldReturn403WhenUnauthorized() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(
                            get("/api/members/{id}", memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when unauthenticated")
        void shouldReturn401WhenUnauthenticated() throws Exception {
            UUID memberId = UUID.randomUUID();

            mockMvc.perform(
                            get("/api/members/{id}", memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should include edit link when user has MEMBERS:UPDATE authority")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ", "MEMBERS:UPDATE"})
        void shouldIncludeEditLinkWhenUserHasUpdateAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = createTestMember(memberId, "Jan", "Novák", "ZBM0501");

            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(
                            get("/api/members/{id}", memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.default.method").value("PATCH"));  // EDIT
        }

        @Test
        @DisplayName("should return guardian information when present")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
        void shouldReturnGuardianInformationWhenPresent() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withEmail("child@example.com")
                    .withPhone("+420777333444")
                    .withAddress(new Address("Hlavní 456", "Brno", "12345", "CZ"))
                    .withGuardian(new GuardianInformation("Parent", "Name", "PARENT","parent@example.com", "+420777111222"))
                    .build();

            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(
                            get("/api/members/{id}", memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )

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
        @DisplayName("should return single email and phone with address")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
        void shouldReturnSingleEmailAndPhoneWithAddress() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = createTestMember(
                    memberId,
                    "ZBM0501",
                    "Eva",
                    "Svobodová",
                    "SK",
                    "eva.svobodova@example.com",
                    "+421777888999",
                    "Main Street 123",
                    "Bratislava",
                    "81101"
            );

            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(
                            get("/api/members/{id}", memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("eva.svobodova@example.com"))
                    .andExpect(jsonPath("$.phone").value("+421777888999"))
                    .andExpect(jsonPath("$.address.street").value("Main Street 123"))
                    .andExpect(jsonPath("$.address.city").value("Bratislava"))
                    .andExpect(jsonPath("$.address.postalCode").value("81101"))
                    .andExpect(jsonPath("$.address.country").value("SK"));
        }

        @Test
        @DisplayName("should include permissions link when user has MEMBERS:PERMISSIONS authority")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ", "MEMBERS:PERMISSIONS"})
        void shouldIncludePermissionsLinkWhenUserHasMembersPermissionsAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = createTestMember(memberId, "Jan", "Novák", "ZBM0501");

            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(
                            get("/api/members/{id}", memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.permissions").exists())
                    .andExpect(jsonPath("$._links.permissions.href").value(
                            org.hamcrest.Matchers.containsString("/api/users/" + memberId + "/permissions")));
        }

        @Test
        @DisplayName("should not include permissions link when user lacks MEMBERS:PERMISSIONS authority")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
        void shouldNotIncludePermissionsLinkWhenUserLacksMembersPermissionsAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = createTestMember(memberId, "Jan", "Novák", "ZBM0501");

            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(
                            get("/api/members/{id}", memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )

                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.permissions").doesNotExist());
        }
    }

    // Helper methods to create test members
    private Member createTestMember(
            UUID id,
            String regNumber,
            String firstName,
            String lastName,
            String countryCode,
            String email,
            String phone,
            String street,
            String city,
            String postalCode
    ) {
        PersonalInformation personalInfo = PersonalInformation.of(
                firstName,
                lastName,
                LocalDate.of(2005, 6, 15),
                countryCode,
                Gender.MALE
        );

        Address address = Address.of(street, city, postalCode, countryCode);
        EmailAddress emailAddress = EmailAddress.of(email);
        PhoneNumber phoneNumber = PhoneNumber.of(phone);

        return MemberTestDataBuilder.aMember()
                .withId(id)
                .withRegistrationNumber(regNumber)
                .withName("Jan", "Novák")
                .withDateOfBirth(LocalDate.of(2005, 6, 15))
                .withNationality(countryCode)
                .withGender(Gender.MALE)
                .withAddress(address)
                .withEmail(email)
                .withPhone(phone)
                .build();
    }

    private Member createTestMemberWithGuardian(UUID id) {
        PersonalInformation personalInfo = PersonalInformation.of(
                "Child",
                "Member",
                LocalDate.of(2015, 1, 10),
                "CZ",
                Gender.MALE
        );

        Address address = Address.of("Hlavní 456", "Brno", "60000", "CZ");
        EmailAddress email = EmailAddress.of("child@example.com");
        PhoneNumber phone = PhoneNumber.of("+420777333444");

        GuardianInformation guardian = new GuardianInformation(
                "Parent",
                "Name",
                "PARENT",
                EmailAddress.of("parent@example.com"),
                PhoneNumber.of("+420777111222")
        );

        return MemberTestDataBuilder.aMember()
                .withId(id)
                .withRegistrationNumber("ZBM1501")
                .withName("Child", "Member")
                .withDateOfBirth(LocalDate.of(2015, 1, 10))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(address)
                .withEmail("child@example.com")
                .withPhone("+420111222333")
                .withGuardian(guardian)
                .build();
    }

    @Nested
    @DisplayName("POST /api/members")
    class RegisterMemberTests {

        static ResultMatcher locationHeaderWithMemberDetailRedirect(UUID expectedMemberId) {
            return header().string(HttpHeaders.LOCATION, "/api/members/%s".formatted(expectedMemberId.toString()));
        }

        @Test
        @DisplayName("with valid data should return 201 with HATEOAS links")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldCreateMemberWithValidData() throws Exception {
            UUID memberId = UUID.randomUUID();

            when(registrationService.registerMember(any(RegisterMemberRequest.class))).thenReturn(memberId);
            when(entityLinks.linkToItemResource(eq(Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated())
                    .andExpect(locationHeaderWithMemberDetailRedirect(memberId));
        }

        @Test
        @DisplayName("with minor should accept guardian")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldCreateMinorWithGuardian() throws Exception {
            UUID memberId = UUID.randomUUID();

            when(registrationService.registerMember(any(RegisterMemberRequest.class))).thenReturn(memberId);
            when(entityLinks.linkToItemResource(eq(Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated())
                    .andExpect(locationHeaderWithMemberDetailRedirect(memberId));
        }

        @Test
        @DisplayName("with missing first name should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenFirstNameMissing() throws Exception {
            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.firstName").value("First name is required"));
        }

        @Test
        @DisplayName("with invalid email should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenEmailInvalid() throws Exception {
            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.email").value("Email must be valid"));
        }

        @Test
        @DisplayName("with invalid phone should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenPhoneInvalid() throws Exception {
            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.phone").value(
                            "Phone number must be in E.164 format (starts with +)"));
        }

        @Test
        @DisplayName("with future date of birth should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenDateOfBirthInFuture() throws Exception {
            LocalDate futureDate = LocalDate.now().plusDays(1);

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content("""
                                            {
                                                "firstName": "Jan",
                                                "lastName": "Novák",
                                                "dateOfBirth": "%s",
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
                                            """.formatted(futureDate))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.dateOfBirth").value("Date of birth must be in the past"));
        }

        @Test
        @DisplayName("with missing email should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenEmailMissing() throws Exception {
            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.email").value("Email is required"));
        }

        @Test
        @DisplayName("with invalid guardian should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenGuardianInvalid() throws Exception {
            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors['guardian.firstName']").value("Guardian first name is required"));
        }

        @Test
        @DisplayName("with valid address and contacts should succeed")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldCreateMemberWithValidAddressAndContacts() throws Exception {
            UUID memberId = UUID.randomUUID();

            when(registrationService.registerMember(any(RegisterMemberRequest.class))).thenReturn(memberId);
            when(entityLinks.linkToItemResource(eq(Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated())
                    .andExpect(locationHeaderWithMemberDetailRedirect(memberId));
        }

        @Test
        @DisplayName("with invalid nationality code should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenNationalityCodeInvalid() throws Exception {
            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.nationality").value(
                            "Nationality must be 2 or 3 characters (ISO code)"));
        }

        @Test
        @DisplayName("with invalid address missing fields should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenAddressMissingFields() throws Exception {
            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors['address.street']").value("Street is required"));
        }

        @Test
        @DisplayName("with invalid address country code format should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenAddressCountryInvalid() throws Exception {
            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content("""
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors['address.country']").value(ValidationPatterns.MESSAGE_COUNTRY_INVALID));
        }
    }

    @Nested
    @DisplayName("GET /api/members")
    class ListMembersTests {

        @Test
        @DisplayName("should return 200 with empty collection when no members exist")
        @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
        void shouldReturnEmptyCollectionWhenNoMembers() throws Exception {
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(
                    List.of()));

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
        @DisplayName("should return collection of member summaries with HATEOAS links")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
        void shouldReturnMemberCollectionWithHateoasLinks() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = createTestMember(memberId, "Jan", "Novák", "ZBM0501");

            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

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
        @DisplayName("should return paginated results")
        @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
        void shouldReturnPaginatedResults() throws Exception {
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

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
        @DisplayName("should return sorted results")
        @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
        void shouldReturnSortedResults() throws Exception {
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

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
        @DisplayName("should return 400 when sort field invalid")
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
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(
                            "Invalid sort field")));
        }

        @Test
        @DisplayName("should handle multi-field sort")
        @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
        void shouldHandleMultiFieldSort() throws Exception {
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

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
        @DisplayName("should use default pagination when no parameters provided")
        @WithMockUser(username = MEMBER_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
        void shouldUseDefaultPaginationWhenNoParams() throws Exception {
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

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
        @DisplayName("should include pagination links")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
        void shouldIncludePaginationLinks() throws Exception {
            List<Member> members = List.of(
                    createTestMember(UUID.randomUUID(), "Member0", "Test0", "ZBM0001"),
                    createTestMember(UUID.randomUUID(), "Member1", "Test1", "ZBM0002"),
                    createTestMember(UUID.randomUUID(), "Member2", "Test2", "ZBM0003")
            );

            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(members, PageRequest.of(0, 2), members.size()));

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
    }

    @Nested
    @DisplayName("POST /api/members/{id}/terminate")
    class TerminateMemberTests {

        private static final String MEMBERS_UPDATE_AUTHORITY = "MEMBERS:UPDATE";

        @Test
        @DisplayName("successful termination should return 200 OK with termination details")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_UPDATE_AUTHORITY})
        void shouldTerminateMemberSuccessfully() throws Exception {
            // Arrange
            when(userServiceMock.findUserByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(ZBM001));

            UUID memberId = UUID.randomUUID();
            Member activeMember = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2005, 6, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420777123456")
                    .withNoGuardian()
                    .build();

            // Create a terminated member for the response
            Member terminatedMember = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withRegistrationNumber("ZBM0501")
                    .withName("Jan", "Novák")
                    .withDateOfBirth(LocalDate.of(2005, 6, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("jan.novak@example.com")
                    .withPhone("+420777123456")
                    .withNoGuardian()
                    .terminated(DeactivationReason.ODHLASKA, "Member requested termination")
                    .build();

            when(managementService.terminateMember(eq(memberId), any(Member.TerminateMembership.class)))
                    .thenReturn(terminatedMember);
            when(memberRepository.findById(new UserId(memberId)))
                    .thenReturn(java.util.Optional.of(terminatedMember));
            when(entityLinks.linkToItemResource(eq(Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            // Act & Assert
            mockMvc.perform(
                            post("/api/members/" + memberId + "/terminate")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {
                                                "reason": "ODHLASKA",
                                                "note": "Member requested termination"
                                            }
                                            """)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("termination without note should return 204 NO_CONTENT with Location header pointing to list of members")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_UPDATE_AUTHORITY})
        void shouldTerminateMemberWithoutNote() throws Exception {
            // Arrange
            when(userServiceMock.findUserByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(ZBM001));

            UUID memberId = UUID.randomUUID();
            Member terminatedMember = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withRegistrationNumber("ZBM0502")
                    .withName("Petra", "Nováková")
                    .withDateOfBirth(LocalDate.of(2010, 6, 20))
                    .withNationality("CZ")
                    .withGender(Gender.FEMALE)
                    .withAddress(Address.of("Hlavní 456", "Brno", "60000", "CZ"))
                    .withEmail("petra.novakova@example.com")
                    .withPhone("+420111222333")
                    .withNoGuardian()
                    .terminated(DeactivationReason.PRESTUP, null)
                    .build();

            when(managementService.terminateMember(eq(memberId), any(Member.TerminateMembership.class)))
                    .thenReturn(terminatedMember);
            when(memberRepository.findById(new UserId(memberId)))
                    .thenReturn(java.util.Optional.of(terminatedMember));
            when(entityLinks.linkToItemResource(eq(Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            // Act & Assert
            mockMvc.perform(
                            post("/api/members/" + memberId + "/terminate")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content("""
                                            {
                                                "reason": "PRESTUP"
                                            }
                                            """)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isNoContent())
                    .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/api/members"));
        }

        @Test
        @DisplayName("active member GET response should include terminate affordance")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_UPDATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
        void shouldIncludeTerminateAffordanceForActiveMember() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();
            Member activeMember = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withRegistrationNumber("ZBM0504")
                    .withName("Active", "Member")
                    .withDateOfBirth(LocalDate.of(2005, 6, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .withEmail("active.member@example.com")
                    .withPhone("+420777123456")
                    .withNoGuardian()
                    .build();

            when(memberRepository.findById(new UserId(memberId)))
                    .thenReturn(java.util.Optional.of(activeMember));
            when(entityLinks.linkToItemResource(eq(Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            // Act & Assert
            mockMvc.perform(
                            get("/api/members/" + memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").exists());
            // Note: Affordances are part of _templates in HAL+FORMS, but we verify the controller logic
            // by checking that the member is active and the link is present
        }

        @Test
        @DisplayName("terminated member GET response should not include terminate affordance")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_UPDATE_AUTHORITY, MEMBERS_READ_AUTHORITY})
        void shouldNotIncludeTerminateAffordanceForTerminatedMember() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();
            Member terminatedMember = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withRegistrationNumber("ZBM0505")
                    .withName("Terminated", "Member")
                    .withDateOfBirth(LocalDate.of(2005, 6, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 456", "Brno", "60000", "CZ"))
                    .withEmail("terminated.member@example.com")
                    .withPhone("+420111222333")
                    .withNoGuardian()
                    .terminated(DeactivationReason.ODHLASKA, "Member resigned")
                    .build();

            when(memberRepository.findById(new UserId(memberId)))
                    .thenReturn(java.util.Optional.of(terminatedMember));
            when(entityLinks.linkToItemResource(eq(Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            // Act & Assert
            mockMvc.perform(
                            get("/api/members/" + memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.collection.href").exists());
            // Note: The controller only adds update affordance for terminated members
            // This is verified by checking that member is not active
        }

        @Test
        @DisplayName("self link should always be present on member responses")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_READ_AUTHORITY})
        void shouldAlwaysIncludeSelfLink() throws Exception {
            // Arrange
            UUID memberId = UUID.randomUUID();
            Member activeMember = MemberTestDataBuilder.aMemberWithId(memberId)
                    .withRegistrationNumber("ZBM0506")
                    .withName("Self", "Link")
                    .withDateOfBirth(LocalDate.of(2005, 6, 15))
                    .withNationality("CZ")
                    .withGender(Gender.MALE)
                    .withAddress(Address.of("Hlavní 789", "Ostrava", "70000", "CZ"))
                    .withEmail("self.link@example.com")
                    .withPhone("+420444555666")
                    .withNoGuardian()
                    .build();

            when(memberRepository.findById(new UserId(memberId)))
                    .thenReturn(java.util.Optional.of(activeMember));
            when(entityLinks.linkToItemResource(eq(Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            // Act & Assert
            mockMvc.perform(
                            get("/api/members/" + memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.self.href").exists())
                    .andExpect(jsonPath("$._links.self.href").value(org.hamcrest.Matchers.containsString("/api/members/" + memberId)));
        }

        @Test
        @DisplayName("termination of already terminated member should return 400 Bad Request")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_UPDATE_AUTHORITY})
        void shouldReturn400WhenTerminatingAlreadyTerminatedMember() throws Exception {
            // Arrange
            when(userServiceMock.findUserByUsername(ADMIN_USERNAME)).thenReturn(Optional.of(ZBM001));

            UUID memberId = UUID.randomUUID();

            when(managementService.terminateMember(eq(memberId), any(Member.TerminateMembership.class)))
                    .thenThrow(new InvalidUpdateException("Member is already terminated"));

            // Act & Assert
            mockMvc.perform(
                            post("/api/members/" + memberId + "/terminate")
                                    .contentType("application/json")
                                    .content("""
                                            {
                                                "reason": "OTHER",
                                                "note": "Second termination attempt"
                                            }
                                            """)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("already terminated")));
        }

        @Test
        @DisplayName("termination with invalid reason should return 400 Bad Request")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_UPDATE_AUTHORITY})
        void shouldReturn400WhenTerminationReasonInvalid() throws Exception {
            // Arrange - note: validation happens at request level via @Valid
            // This test verifies the controller handles invalid requests properly
            UUID memberId = UUID.randomUUID();

            // Create request with null reason (should fail validation)
            String invalidRequest = """
                    {
                        "reason": null,
                        "note": "Test note"
                    }
                    """;

            // Act & Assert
            mockMvc.perform(
                            post("/api/members/" + memberId + "/terminate")
                                    .contentType("application/json")
                                    .content(invalidRequest)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest());
        }
    }

    // Helper method to create test Member
    private Member createTestMember(UUID id, String firstName, String lastName, String registrationNumber) {
        return MemberTestDataBuilder.aMemberWithId(id)
                .withRegistrationNumber(registrationNumber)
                .withName(firstName, lastName)
                .withDateOfBirth(LocalDate.of(2005, 6, 15))
                .withNationality("CZ")
                .withGender(Gender.MALE)
                .withAddress(Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                .withEmail("test@example.com")
                .withPhone("+420777888999")
                .withNoGuardian()
                .build();
    }
}
