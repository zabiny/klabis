package com.klabis.membershipfees;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.AuthorizationSnapshotLoader;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.finance.domain.Money;
import com.klabis.groups.freegroup.application.FreeGroupManagementPort;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.InvitationId;
import com.klabis.members.MemberId;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.Country;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.Nationality;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.FeeYearLifecyclePort;
import com.klabis.membershipfees.application.MembershipFeeTypeManagementPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Fee type choice through MEMBERS:SELECT_MEMBERSHIP_FEE_TYPE — full-stack integration")
class MemberFeeSelfAccessIntegrationTest {

    private static final String ADMIN_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final Money YEARLY_FEE = Money.of(new BigDecimal("1200.00"), Currency.getInstance("CZK"));
    private static final int YEAR = LocalDate.now().getYear();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private MembershipFeeTypeManagementPort typeManagementPort;
    @Autowired
    private FeeSelectionCampaignManagementPort campaignManagementPort;
    @Autowired
    private FeeYearLifecyclePort feeYearLifecyclePort;
    @Autowired
    private AuthorizationSnapshotLoader snapshotLoader;
    @Autowired
    private FreeGroupManagementPort freeGroupPort;
    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;

    private String groupId;
    private Member adult;
    private Member minor;
    private Member guardianOfMinor;
    private Member groupOwner;
    private Member groupMember;

    @BeforeEach
    void publishedCampaignAndPeople() {
        if (campaignManagementPort.findActiveCampaign().isEmpty()) {
            var typeId = typeManagementPort.createFeeType(
                    new MembershipFeeTypeManagementPort.CreateTypeCommand("Závodní", YEARLY_FEE));
            campaignManagementPort.publishYear(new FeeSelectionCampaignManagementPort.PublishYearCommand(
                    YEAR, LocalDate.now().plusDays(30), List.of(typeId)));
        }
        groupId = campaignManagementPort.listGroupsForYear(YEAR).getFirst().getId().value().toString();

        adult = registerAdult("Jan");
        guardianOfMinor = registerAdult("Eva");
        groupOwner = registerAdult("Petr");
        groupMember = registerAdult("Karel");
        minor = registerMinor(guardianOfMinor);
    }

    private Member registerAdult(String first) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(first, "Novák", LocalDate.of(1985, 5, 5), Nationality.of("CZ"), Gender.FEMALE),
                Address.of("Hlavní 1", "Praha", "11000", Country.of("CZ")),
                EmailAddress.of(first.toLowerCase() + "." + UUID.randomUUID() + "@example.com"),
                PhoneNumber.of("+420777111222"),
                BirthNumber.of("855505/1234"),
                null, null, List.of(), null));
    }

    private Member registerMinor(Member guardian) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Sofie", "Svobodová", LocalDate.now().minusYears(12), Nationality.of("CZ"),
                        Gender.FEMALE),
                Address.of("Dětská 1", "Praha", "11000", Country.of("CZ")),
                null, null, BirthNumber.of("900101/1234"), null, null,
                List.of(LegalGuardianPort.GuardianInput.existing(guardian.getId().toUserId())), null));
    }

    private Member registerMinorOfNonMemberGuardian() {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Tereza", "Dlouhá", LocalDate.now().minusYears(10), Nationality.of("CZ"),
                        Gender.FEMALE),
                Address.of("Dětská 2", "Praha", "11000", Country.of("CZ")),
                null, null, BirthNumber.of("900101/1234"), null, null,
                List.of(LegalGuardianPort.GuardianInput.created(new LegalGuardianPort.NewLegalGuardian("Ivana",
                        "Dlouhá", "ivana." + UUID.randomUUID() + "@example.com", "+420604100100"))), null));
    }

    private RequestPostProcessor loggedInAsNonMember(UserId userId) {
        JwtParams params = JwtParams.jwtTokenParams("guardian", userId);
        return authentication(KlabisAuthenticationFactory.createAuthenticationToken(params,
                snapshotLoader.loadFor(userId)));
    }

    private RequestPostProcessor loggedInAs(Member member) {
        UserId userId = member.getId().toUserId();
        JwtParams params = JwtParams.jwtTokenParams("user", userId).withMemberId(member.getId());
        return authentication(KlabisAuthenticationFactory.createAuthenticationToken(params,
                snapshotLoader.loadFor(userId)));
    }

    // leaves the request untouched so the user from @WithKlabisMockUser applies
    private static final RequestPostProcessor AS_ANNOTATED_USER = request -> request;

    private ResultActions getChoice(Member target, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}/fee-choice/{year}", target.getId().uuid(), YEAR).with(user)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions getSummary(Member target, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}/fee-summary", target.getId().uuid()).with(user)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions getHistory(Member target, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}/fee-history", target.getId().uuid()).with(user)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions getDetail(Member target, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}", target.getId().uuid()).with(user)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private void chooseFeeType(Member target, RequestPostProcessor user, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/members/{id}/fee-choice/{year}", target.getId().uuid(), YEAR).with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipFeeGroupId\":\"" + groupId + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private void removeChoice(Member target, RequestPostProcessor user, int expectedStatus) throws Exception {
        mockMvc.perform(delete("/api/members/{id}/fee-choice/{year}", target.getId().uuid(), YEAR).with(user))
                .andExpect(status().is(expectedStatus));
    }

    private void groupOwnerDelegates(Set<Authority> delegated) {
        FreeGroup group = freeGroupPort.createGroup("Přátelé", groupOwner.getId(), delegated);
        freeGroupPort.inviteMember(group.getId(), groupOwner.getId(), groupMember.getId());
        InvitationId invitation = freeGroupPort.getGroup(group.getId()).getPendingInvitations().stream()
                .filter(inv -> inv.isForMember(groupMember.getId()))
                .findFirst().orElseThrow().getId();
        freeGroupPort.acceptInvitation(group.getId(), invitation, groupMember.getId());
    }

    @Test
    @DisplayName("adult member reads and chooses their own type and sees the fee page link on their detail")
    void adultReadsAndChoosesOwnType() throws Exception {
        getDetail(adult, loggedInAs(adult)).andExpect(jsonPath("$._links.feeHistory").exists());
        getHistory(adult, loggedInAs(adult)).andExpect(status().isOk());
        getChoice(adult, loggedInAs(adult)).andExpect(status().isOk());
        getSummary(adult, loggedInAs(adult)).andExpect(status().isOk());

        chooseFeeType(adult, loggedInAs(adult), 204);
        getSummary(adult, loggedInAs(adult))
                .andExpect(jsonPath("$.currentGroup.id").value(groupId))
                .andExpect(jsonPath("$._templates.removeChoice").exists());
        removeChoice(adult, loggedInAs(adult), 204);
    }

    @Test
    @DisplayName("minor is denied reading and choosing and has no fee page link on their own detail")
    void minorIsDeniedEverything() throws Exception {
        getDetail(minor, loggedInAs(minor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeHistory").doesNotExist());
        getHistory(minor, loggedInAs(minor)).andExpect(status().isForbidden());
        getChoice(minor, loggedInAs(minor)).andExpect(status().isForbidden());
        getSummary(minor, loggedInAs(minor)).andExpect(status().isForbidden());
        chooseFeeType(minor, loggedInAs(minor), 403);
        removeChoice(minor, loggedInAs(minor), 403);
    }

    @Test
    @DisplayName("member guardian reads and chooses for their minor and sees the fee page link on the minor's detail")
    void guardianReadsAndChoosesForMinor() throws Exception {
        getDetail(minor, loggedInAs(guardianOfMinor)).andExpect(jsonPath("$._links.feeHistory").exists());
        getHistory(minor, loggedInAs(guardianOfMinor)).andExpect(status().isOk());
        getChoice(minor, loggedInAs(guardianOfMinor)).andExpect(status().isOk());
        chooseFeeType(minor, loggedInAs(guardianOfMinor), 204);
        removeChoice(minor, loggedInAs(guardianOfMinor), 204);
    }

    @Test
    @DisplayName("administrator reads the fee pages but cannot choose and has no choice affordances")
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void administratorReadsButCannotChoose() throws Exception {
        feeYearLifecyclePort.chooseFeeLevel(new FeeYearLifecyclePort.ChooseFeeLevel(
                adult.getId(), new MembershipFeeGroupId(UUID.fromString(groupId)), YEAR));

        getDetail(adult, AS_ANNOTATED_USER).andExpect(jsonPath("$._links.feeHistory").exists());
        getHistory(adult, AS_ANNOTATED_USER).andExpect(status().isOk());
        getChoice(adult, AS_ANNOTATED_USER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.chooseFeeType").doesNotExist())
                .andExpect(jsonPath("$._templates.removeChoice").doesNotExist());
        getSummary(adult, AS_ANNOTATED_USER).andExpect(status().isOk());
        chooseFeeType(adult, AS_ANNOTATED_USER, 403);
        removeChoice(adult, AS_ANNOTATED_USER, 403);
    }

    @Test
    @DisplayName("owner of a free group delegating profile editing neither reads nor chooses for a group member")
    void freeGroupOwnerIsDenied() throws Exception {
        groupOwnerDelegates(Set.of(Authority.MEMBERS_EDIT_PROFILE));

        getDetail(groupMember, loggedInAs(groupOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeHistory").doesNotExist());
        getHistory(groupMember, loggedInAs(groupOwner)).andExpect(status().isForbidden());
        getChoice(groupMember, loggedInAs(groupOwner)).andExpect(status().isForbidden());
        getSummary(groupMember, loggedInAs(groupOwner)).andExpect(status().isForbidden());
        chooseFeeType(groupMember, loggedInAs(groupOwner), 403);
        removeChoice(groupMember, loggedInAs(groupOwner), 403);
    }

    @Test
    @DisplayName("non-member guardian reads and chooses for their minor and sees the fee page link")
    void nonMemberGuardianReadsAndChoosesForMinor() throws Exception {
        Member minorOfExt = registerMinorOfNonMemberGuardian();
        UserId extGuardianId = legalGuardianGroupPort.guardiansOf(minorOfExt.getId()).iterator().next();
        RequestPostProcessor ext = loggedInAsNonMember(extGuardianId);

        getDetail(minorOfExt, ext).andExpect(jsonPath("$._links.feeHistory").exists());
        getHistory(minorOfExt, ext).andExpect(status().isOk());
        getChoice(minorOfExt, ext).andExpect(status().isOk());
        getSummary(minorOfExt, ext).andExpect(status().isOk());
        chooseFeeType(minorOfExt, ext, 204);
        removeChoice(minorOfExt, ext, 204);
    }

    @Test
    @DisplayName("guardians are denied reading and choosing for a minor who is not theirs")
    void guardiansAreDeniedForForeignMinor() throws Exception {
        Member minorOfExt = registerMinorOfNonMemberGuardian();
        UserId extGuardianId = legalGuardianGroupPort.guardiansOf(minorOfExt.getId()).iterator().next();

        for (var denied : List.of(
                new Object[]{minor, loggedInAsNonMember(extGuardianId)},
                new Object[]{minorOfExt, loggedInAs(guardianOfMinor)})) {
            Member target = (Member) denied[0];
            RequestPostProcessor guardian = (RequestPostProcessor) denied[1];
            getHistory(target, guardian).andExpect(status().isForbidden());
            getChoice(target, guardian).andExpect(status().isForbidden());
            getSummary(target, guardian).andExpect(status().isForbidden());
            chooseFeeType(target, guardian, 403);
            removeChoice(target, guardian, 403);
            getDetail(target, guardian).andExpect(jsonPath("$._links.feeHistory").doesNotExist());
        }
    }
}
