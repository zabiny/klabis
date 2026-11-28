package com.klabis.members.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.members.MemberTestDataBuilder;
import com.klabis.members.MembersWebMvcTest;
import com.klabis.members.application.EffectiveContact;
import com.klabis.members.application.MemberContactPort;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.domain.RegistrationNumber;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncPairing;
import com.klabis.sync.domain.SyncTarget;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/members")
@MembersWebMvcTest
class MemberListApiTest extends MemberApiTestBase {

    @Autowired
    private MemberContactPort memberContactPort;
    private MockHttpServletRequestBuilder getApiMembers() {
        return get("/api/members")
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE);
    }

    @Test
    @DisplayName("should return 200 with empty collection when no members exist")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldReturnEmptyCollectionWhenNoMembers() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of()));

        mockMvc.perform(getApiMembers())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList").doesNotExist())
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    private Member incompleteAdultWithEmail() {
        return MemberTestDataBuilder.aMemberWithId(UUID.randomUUID())
                .withEmail("jan.novak@example.com")
                .withActive(true)
                .withDateOfBirth(LocalDate.of(1990, 1, 1))
                .withPhone((PhoneNumber) null)
                .withDataIncomplete(true)
                .build();
    }

    private Member minorWithoutContact() {
        Member member = MemberTestDataBuilder.aMemberWithId(UUID.randomUUID())
                .withDateOfBirth(LocalDate.now().minusYears(10))
                .withEmail((EmailAddress) null)
                .withPhone((PhoneNumber) null)
                .build();
        when(memberContactPort.effectiveContactOf(eq(member))).thenReturn(new EffectiveContact(
                new EffectiveContact.Value("jan@example.com", "Jan Novák"),
                EffectiveContact.Value.EMPTY));
        return member;
    }

    private void listReturns(Member... members) {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(members), PageRequest.of(0, 10), members.length));
    }

    private static String item(Member member, String path) {
        return "$._embedded.memberSummaryResponseList[?(@.id=='%s')].%s".formatted(member.getId().uuid(), path);
    }

    @Test
    @DisplayName("caller with MEMBERS:MANAGE sees email, active, dataIncomplete, guardian fallback contact and the suspend template in summary items")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void manageCallerSeesManagementFieldsInSummaryItems() throws Exception {
        Member adult = incompleteAdultWithEmail();
        Member minor = minorWithoutContact();
        listReturns(adult, minor);

        mockMvc.perform(getApiMembers())
                .andExpect(status().isOk())
                .andExpect(jsonPath(item(adult, "email")).value(Matchers.hasItem("jan.novak@example.com")))
                .andExpect(jsonPath(item(adult, "active")).value(Matchers.hasItem(true)))
                .andExpect(jsonPath(item(adult, "dataIncomplete")).value(Matchers.hasItem(true)))
                .andExpect(jsonPath(item(adult, "_templates.suspendMember.method")).value(Matchers.hasItem("POST")))
                .andExpect(jsonPath(item(minor, "displayContact.email.value")).value(Matchers.hasItem("jan@example.com")))
                .andExpect(jsonPath(item(minor, "displayContact.email.fromGuardian")).value(Matchers.hasItem(true)))
                .andExpect(jsonPath(item(minor, "displayContact.email.guardianName")).value(Matchers.hasItem("Jan Novák")))
                .andExpect(jsonPath(item(minor, "displayContact.phone.value")).doesNotExist());
    }

    @Test
    @DisplayName("caller without MEMBERS:MANAGE does not see email, active, dataIncomplete, displayContact or action templates in summary items")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void regularCallerDoesNotSeeManagementFieldsInSummaryItems() throws Exception {
        Member adult = incompleteAdultWithEmail();
        Member minor = minorWithoutContact();
        listReturns(adult, minor);

        mockMvc.perform(getApiMembers())
                .andExpect(status().isOk())
                .andExpect(jsonPath(item(adult, "id")).value(Matchers.hasItem(adult.getId().uuid().toString())))
                .andExpect(jsonPath(item(adult, "email")).doesNotExist())
                .andExpect(jsonPath(item(adult, "active")).doesNotExist())
                .andExpect(jsonPath(item(adult, "dataIncomplete")).doesNotExist())
                .andExpect(jsonPath(item(adult, "_templates")).doesNotExist())
                .andExpect(jsonPath(item(minor, "id")).value(Matchers.hasItem(minor.getId().uuid().toString())))
                .andExpect(jsonPath(item(minor, "displayContact")).doesNotExist());
    }

    @Test
    @DisplayName("should call service with default page 0, size 10 and sort by lastName then firstName ascending")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldCallRepositoryWithDefaultPagination() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(getApiMembers());

        Mockito.verify(managementService).listMembers(any(MemberFilter.class), argThat(pageable ->
                pageable.getPageNumber() == 0 &&
                pageable.getPageSize() == 10 &&
                pageable.getSort().getOrderFor("lastName") != null &&
                pageable.getSort().getOrderFor("lastName").isAscending() &&
                pageable.getSort().getOrderFor("firstName") != null &&
                pageable.getSort().getOrderFor("firstName").isAscending()
        ));
    }

    @Test
    @DisplayName("should pass explicit page, size and sort parameters to the service")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void shouldCallRepositoryWithCorrectSortParameters() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(getApiMembers()
                .param("page", "2")
                .param("size", "20")
                .param("sort", "firstName,desc")
                .param("sort", "registrationNumber,asc"));

        Mockito.verify(managementService).listMembers(any(MemberFilter.class), argThat(pageable ->
                pageable.getPageNumber() == 2 &&
                pageable.getPageSize() == 20 &&
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

        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member1, member2), PageRequest.of(0, 10), 2));

        mockMvc.perform(getApiMembers())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[*].id")
                        .value(Matchers.contains(memberId1.toString(), memberId2.toString())))
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[*]._links.self.href")
                        .value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(1))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$._links.self.href").exists());
    }

    @Test
    @DisplayName("HAL+FORMS: user with MEMBERS_MANAGE should see registerMember template with gender and legal guardian options on collection")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void adminShouldSeeRegisterMemberTemplateOnCollection() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(getApiMembers())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.registerMember").exists())
                .andExpect(jsonPath("$._templates.registerMember.method").value("POST"))
                .andExpect(jsonPath("$._templates.registerMember.properties[?(@.name=='gender')].options.inline")
                        .value(Matchers.hasItem(Matchers.hasItems("MALE", "FEMALE"))))
                .andExpect(jsonPath("$._templates.registerMember.properties[?(@.name=='gender')].type").value("Gender"))
                .andExpect(jsonPath("$._templates.registerMember.properties[?(@.name=='legalGuardians')].options.link.href")
                        .value(Matchers.hasItem(Matchers.containsString("/api/legal-guardian-options"))))
                .andExpect(jsonPath("$._templates.registerMember.properties[?(@.name=='legalGuardianUserId')].options.link.href")
                        .value(Matchers.hasItem(Matchers.containsString("kind=LEGAL_GUARDIAN"))));
    }

    @Test
    @DisplayName("HAL+FORMS: collection updateMember template carries inline gender and drivingLicenseGroup options")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void collectionUpdateMemberTemplateCarriesGenderAndDrivingLicenseInlineOptions() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(getApiMembers())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='gender')].options.inline")
                        .value(Matchers.hasItem(Matchers.hasItems("MALE", "FEMALE"))))
                .andExpect(jsonPath("$._templates.updateMember.properties[?(@.name=='drivingLicenseGroup')].options.inline")
                        .value(Matchers.hasItem(Matchers.hasItems("AM", "T"))));
    }

    @Test
    @DisplayName("HAL+FORMS: user without MEMBERS_MANAGE should not see registerMember template on collection")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void memberShouldNotSeeRegisterMemberTemplateOnCollection() throws Exception {
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(getApiMembers())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.registerMember").doesNotExist());
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
                .andExpect(jsonPath("$.detail").value(Matchers.containsString(
                        "Invalid sort field")));
    }

    @Test
    @DisplayName("HAL+FORMS: user with MEMBERS:PERMISSIONS should see permissions link on active summary item")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_PERMISSIONS})
    void userWithPermissionsAuthorityShouldSeePermissionsLinkOnActiveSummaryItem() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(true)
                .build();
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

        mockMvc.perform(getApiMembers())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._links.permissions.href")
                        .value("http://localhost/api/users/" + memberId + "/permissions"));
    }

    @Test
    @DisplayName("HAL+FORMS: user with MEMBERS:PERMISSIONS should not see permissions link on inactive summary item")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_PERMISSIONS})
    void userWithPermissionsAuthorityShouldNotSeePermissionsLinkOnInactiveSummaryItem() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(false)
                .build();
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

        mockMvc.perform(getApiMembers())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._links.permissions").doesNotExist());
    }

    @Test
    @DisplayName("HAL+FORMS: user without MEMBERS:PERMISSIONS should not see permissions link on summary items")
    @WithKlabisMockUser(username = MEMBER_USERNAME, authorities = {Authority.MEMBERS_READ})
    void userWithoutPermissionsAuthorityShouldNotSeePermissionsLinkOnSummaryItems() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member member = MemberTestDataBuilder.aMemberWithId(memberId)
                .withActive(true)
                .build();
        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1));

        mockMvc.perform(getApiMembers())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._links.permissions").doesNotExist());
    }


    private static String link(String name) {
        return "$._embedded.memberSummaryResponseList[0]._links." + name + ".href";
    }

    @Test
    @DisplayName("ORIS-enrolled member row carries a sync link")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
    void enrolledRowCarriesSyncLink() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member orisMember = MemberTestDataBuilder.aMemberWithId(memberId).build();

        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orisMember), PageRequest.of(0, 10), 1));

        SyncTarget target = new SyncTarget(SyncEntityType.MEMBER, memberId.toString());
        when(pairingLookup.pairingOf(target)).thenReturn(SyncPairing.ACTIVE);

        mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath(link("sync")).exists());

        Mockito.verify(pairingLookup, Mockito.times(1)).pairingOf(target);
    }

    @Test
    @DisplayName("hand-registered member row does NOT carry a sync link")
    @WithKlabisMockUser(username = ADMIN_USERNAME, authorities = {Authority.MEMBERS_READ})
    void nonEnrolledRowDoesNotCarrySyncLink() throws Exception {
        UUID memberId = UUID.randomUUID();
        Member handRegisteredMember = MemberTestDataBuilder.aMemberWithId(memberId).build();

        when(managementService.listMembers(any(MemberFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(handRegisteredMember), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/members").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.memberSummaryResponseList[0]._links.sync").doesNotExist());
    }
}
