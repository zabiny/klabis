package com.klabis.members.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.config.encryption.EncryptionConfiguration;
import com.klabis.members.*;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.LocalDate;
import java.util.List;
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

    private static final String ADMIN_USERNAME = "admin";
    private static final String MEMBER_USERNAME = "ZBM0101";
    private static final String MEMBERS_READ_AUTHORITY = "MEMBERS:READ";
    private static final String MEMBERS_CREATE_AUTHORITY = "MEMBERS:CREATE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Nested
    @DisplayName("POST /api/members")
    class RegisterMemberTests {

        @Test
        @DisplayName("with valid data should return 201 with HATEOAS links")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldCreateMemberWithValidData() throws Exception {
            UUID memberId = UUID.randomUUID();
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    MemberManagementDtosTestDataBuilder.DEFAULT_ADULT_DATE_OF_BIRTH,
                    "CZ",
                    Gender.MALE,
                    "jan.novak@example.com",
                    "+420777123456",
                    MemberManagementDtosTestDataBuilder.defaultAddressRequest(),
                    null,
                    null,
                    null
            );

            when(registrationService.registerMember(any(RegisterMemberRequest.class))).thenReturn(memberId);
            when(entityLinks.linkToItemResource(eq(com.klabis.members.Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.id").value(memberId.toString()))
                    .andExpect(jsonPath("$.firstName").value("Jan"))
                    .andExpect(jsonPath("$.lastName").value("Novák"))
                    .andExpect(jsonPath("$._links.self.href").exists());
        }

        @Test
        @DisplayName("with minor should accept guardian")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldCreateMinorWithGuardian() throws Exception {
            UUID memberId = UUID.randomUUID();
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Petra",
                    "Nováková",
                    LocalDate.of(2010, 6, 20),
                    "CZ",
                    Gender.FEMALE,
                    "petra.novakova@example.com",
                    "+420111222333",
                    MemberManagementDtosTestDataBuilder.addressRequestWithStreetAndCity("Hlavní 456", "Brno"),
                    MemberManagementDtosTestDataBuilder.defaultGuardianDto(), null, null
            );

            when(registrationService.registerMember(any(RegisterMemberRequest.class))).thenReturn(memberId);
            when(entityLinks.linkToItemResource(eq(com.klabis.members.Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isString());
        }

        @Test
        @DisplayName("with missing first name should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenFirstNameMissing() throws Exception {
            RegisterMemberRequest request = new RegisterMemberRequest(
                    "",
                    "Novák",
                    MemberManagementDtosTestDataBuilder.DEFAULT_ADULT_DATE_OF_BIRTH,
                    "CZ",
                    Gender.MALE,
                    "jan@example.com",
                    "+420777123456",
                    MemberManagementDtosTestDataBuilder.defaultAddressRequest(),
                    null,
                    null,
                    null
            );

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
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
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );

            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 5, 15),
                    "CZ",
                    Gender.MALE,
                    "invalid-email",
                    "+420777123456",
                    address,
                    null, null, null
            );

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
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
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );

            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 5, 15),
                    "CZ",
                    Gender.MALE,
                    "jan@example.com",
                    "123",
                    address,
                    null, null, null
            );

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.phone").value("Phone number must be in E.164 format (starts with +)"));
        }

        @Test
        @DisplayName("with future date of birth should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenDateOfBirthInFuture() throws Exception {
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );

            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    LocalDate.now().plusDays(1),
                    "CZ",
                    Gender.MALE,
                    "jan@example.com",
                    "+420777123456",
                    address,
                    null, null, null
            );

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
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
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );

            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 5, 15),
                    "CZ",
                    Gender.MALE,
                    "",
                    "+420777123456",
                    address,
                    null, null, null
            );

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
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
            GuardianDTO guardian = new GuardianDTO(
                    "",
                    "Novák",
                    "PARENT",
                    "pavel.novak@example.com",
                    "+420987654321"
            );

            AddressRequest address = new AddressRequest(
                    "Hlavní 456",
                    "Brno",
                    "60000",
                    "CZ"
            );

            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Petra",
                    "Nováková",
                    LocalDate.of(2010, 6, 20),
                    "CZ",
                    Gender.FEMALE,
                    "petra@example.com",
                    "+420111222333",
                    address,
                    guardian, null, null
            );

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
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
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );

            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 5, 15),
                    "CZ",
                    Gender.MALE,
                    "jan.novak@example.com",
                    "+420777123456",
                    address,
                    null, null, null
            );

            when(registrationService.registerMember(any(RegisterMemberRequest.class))).thenReturn(memberId);
            when(entityLinks.linkToItemResource(eq(com.klabis.members.Member.class), eq(memberId)))
                    .thenReturn(Link.of("/api/members/" + memberId));

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isString());
        }

        @Test
        @DisplayName("with invalid nationality code should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenNationalityCodeInvalid() throws Exception {
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "CZ"
            );

            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 5, 15),
                    "CZECH",
                    Gender.MALE,
                    "jan@example.com",
                    "+420777123456",
                    address,
                    null, null, null
            );

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
                    )
                    .andDo(MockMvcResultHandlers.print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("about:blank"))
                    .andExpect(jsonPath("$.title").value("Bad Request"))
                    .andExpect(jsonPath("$.fieldErrors.nationality").value("Nationality must be 2 or 3 characters (ISO code)"));
        }

        @Test
        @DisplayName("with invalid address missing fields should return 400")
        @WithMockUser(username = ADMIN_USERNAME, authorities = {MEMBERS_CREATE_AUTHORITY})
        void shouldReturn400WhenAddressMissingFields() throws Exception {
            AddressRequest address = new AddressRequest(
                    "",
                    "Praha",
                    "11000",
                    "CZ"
            );

            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 5, 15),
                    "CZ",
                    Gender.MALE,
                    "jan@example.com",
                    "+420777123456",
                    address,
                    null, null, null
            );

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
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
            AddressRequest address = new AddressRequest(
                    "Hlavní 123",
                    "Praha",
                    "11000",
                    "X"
            );

            RegisterMemberRequest request = new RegisterMemberRequest(
                    "Jan",
                    "Novák",
                    LocalDate.of(2005, 5, 15),
                    "CZ",
                    Gender.MALE,
                    "jan@example.com",
                    "+420777123456",
                    address,
                    null, null, null
            );

            mockMvc.perform(
                            post("/api/members")
                                    .contentType("application/json")
                                    .content(objectMapper.writeValueAsString(request))
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
            when(memberRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of()));

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
