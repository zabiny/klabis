package com.klabis.members.management;

import com.klabis.members.*;
import com.klabis.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API tests for GET /api/members/{id} endpoint.
 */
@DisplayName("Get Member Controller API Tests")
@WebMvcTest(controllers = MemberController.class)
class GetMemberApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManagementService managementService;

    @MockitoBean
    private Members memberRepository;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Nested
    @DisplayName("GET /api/members/{id}")
    class GetMemberTests {

        @Test
        @DisplayName("should return 200 with member details")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
        void shouldReturnMemberDetailsWhenFound() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = createTestMember(memberId, "ZBM0501", "Jan", "Novák");

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
                    .andDo(MockMvcResultHandlers.print())
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
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should include edit link when user has MEMBERS:UPDATE authority")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ", "MEMBERS:UPDATE"})
        void shouldIncludeEditLinkWhenUserHasUpdateAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = createTestMember(memberId, "ZBM0501", "Jan", "Novák");

            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(
                            get("/api/members/{id}", memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._templates.default.method").value("PATCH"));  // EDIT
        }

        @Test
        @DisplayName("should return guardian information when present")
        @WithMockUser(username = "ZBM0001", authorities = {"MEMBERS:READ"})
        void shouldReturnGuardianInformationWhenPresent() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = createTestMemberWithGuardian(memberId);

            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(member));

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
                    .andDo(MockMvcResultHandlers.print())
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
        @WithMockUser(username = "admin", authorities = {"MEMBERS:READ", "MEMBERS:PERMISSIONS"})
        void shouldIncludePermissionsLinkWhenUserHasMembersPermissionsAuthority() throws Exception {
            UUID memberId = UUID.randomUUID();
            Member member = createTestMember(memberId, "ZBM0501", "Jan", "Novák");

            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(
                            get("/api/members/{id}", memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andDo(MockMvcResultHandlers.print())
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
            Member member = createTestMember(memberId, "ZBM0501", "Jan", "Novák");

            when(memberRepository.findById(any(UserId.class))).thenReturn(Optional.of(member));

            mockMvc.perform(
                            get("/api/members/{id}", memberId)
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.permissions").doesNotExist());
        }
    }

    // Helper methods to create test members
    private Member createTestMember(UUID id, String regNumber, String firstName, String lastName) {
        return createTestMember(id, regNumber, firstName, lastName, "CZ",
                "jan.novak@example.com", "+420777888999",
                "Hlavní 123", "Praha", "11000");
    }

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

        return Member.createWithId(
                new UserId(id),
                new RegistrationNumber(regNumber),
                personalInfo,
                address,
                emailAddress,
                phoneNumber,
                null
        );
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

        return Member.createWithId(
                new UserId(id),
                new RegistrationNumber("ZBM1501"),
                personalInfo,
                address,
                email,
                phone,
                guardian
        );
    }
}
