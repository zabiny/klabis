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
import com.klabis.members.MemberId;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.Nationality;
import com.klabis.members.domain.Country;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.FeeYearLifecyclePort;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
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
@DisplayName("Fee level choice on behalf of a minor — full-stack integration")
class MemberFeeChoiceGuardianAccessIntegrationTest {

    private static final String ADMIN_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final Money YEARLY_FEE = Money.of(new BigDecimal("1200.00"), Currency.getInstance("CZK"));
    private static final int YEAR = LocalDate.now().getYear();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private MembershipFeeTierManagementPort tierManagementPort;
    @Autowired
    private FeeSelectionCampaignManagementPort campaignManagementPort;
    @Autowired
    private FeeYearLifecyclePort feeYearLifecyclePort;
    @Autowired
    private AuthorizationSnapshotLoader snapshotLoader;
    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;

    private Member otherMember;
    private Member minor;
    private Member memberGuardian;
    private UserId nonMemberGuardianId;
    private Member minorOfNonMemberGuardian;
    private String groupId;

    @BeforeEach
    void publishedCampaignAndMinors() {
        if (campaignManagementPort.findActiveCampaign().isEmpty()) {
            var tierId = tierManagementPort.createTier(
                    new MembershipFeeTierManagementPort.CreateTierCommand("Závodní", YEARLY_FEE));
            campaignManagementPort.publishYear(new FeeSelectionCampaignManagementPort.PublishYearCommand(
                    YEAR, LocalDate.now().plusDays(30), List.of(tierId)));
        }
        groupId = campaignManagementPort.listGroupsForYear(YEAR).getFirst().getId().value().toString();

        otherMember = registerAdult("Jan", "Novák", "+420777111222");
        memberGuardian = registerAdult("Eva", "Svobodová", "+420777111333");
        minor = registerMinor(List.of(LegalGuardianPort.GuardianInput.existing(memberGuardian.getId().toUserId())));
        minorOfNonMemberGuardian = registerMinor(List.of(LegalGuardianPort.GuardianInput.created(
                new LegalGuardianPort.NewLegalGuardian("Ivana", "Dlouhá",
                        "ivana." + UUID.randomUUID() + "@example.com", "+420604100100"))));
        nonMemberGuardianId = legalGuardianGroupPort.guardiansOf(minorOfNonMemberGuardian.getId())
                .iterator().next();
    }

    private Member registerAdult(String first, String last, String phone) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(first, last, LocalDate.of(1985, 5, 5), Nationality.of("CZ"), Gender.FEMALE),
                Address.of("Hlavní 1", "Praha", "11000", Country.of("CZ")),
                EmailAddress.of(first.toLowerCase() + "." + UUID.randomUUID() + "@example.com"),
                PhoneNumber.of(phone),
                BirthNumber.of("855505/1234"),
                null, null, List.of(), null));
    }

    private Member registerMinor(List<LegalGuardianPort.GuardianInput> guardians) {
        return registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Sofie", "Svobodová", LocalDate.now().minusYears(12), Nationality.of("CZ"), Gender.FEMALE),
                Address.of("Dětská 1", "Praha", "11000", Country.of("CZ")),
                null, null, BirthNumber.of("900101/1234"), null, null, guardians, null));
    }

    private RequestPostProcessor loggedInAs(UserId userId, MemberId memberId) {
        JwtParams params = JwtParams.jwtTokenParams("guardian", userId).withMemberId(memberId);
        return authentication(KlabisAuthenticationFactory.createAuthenticationToken(params,
                snapshotLoader.loadFor(userId)));
    }

    private static final RequestPostProcessor CURRENT_USER = request -> request;

    private RequestPostProcessor memberGuardian() {
        return loggedInAs(memberGuardian.getId().toUserId(), memberGuardian.getId());
    }

    private RequestPostProcessor nonMemberGuardian() {
        return loggedInAs(nonMemberGuardianId, null);
    }

    private ResultActions getSummary(Member target, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}/fee-summary/{year}", target.getId().uuid(), YEAR).with(user)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions getChoice(Member target, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}/fee-choice/{year}", target.getId().uuid(), YEAR).with(user)
                .accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private void chooseTier(Member target, RequestPostProcessor user, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/members/{id}/fee-choice/{year}", target.getId().uuid(), YEAR).with(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"membershipFeeGroupId\":\"" + groupId + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private void removeChoice(Member target, RequestPostProcessor user, int expectedStatus) throws Exception {
        mockMvc.perform(delete("/api/members/{id}/fee-choice/{year}", target.getId().uuid(), YEAR).with(user))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    @DisplayName("member guardian chooses and withdraws the level of their minor")
    void memberGuardianChoosesAndWithdraws() throws Exception {
        chooseTier(minor, memberGuardian(), 204);
        getSummary(minor, memberGuardian())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGroup.id").value(groupId))
                .andExpect(jsonPath("$._templates.chooseTier").exists())
                .andExpect(jsonPath("$._templates.removeChoice").exists());

        removeChoice(minor, memberGuardian(), 204);
        getSummary(minor, memberGuardian())
                .andExpect(jsonPath("$.currentGroup").doesNotExist());
    }

    @Test
    @DisplayName("non-member guardian chooses and withdraws the level of their minor")
    void nonMemberGuardianChoosesAndWithdraws() throws Exception {
        chooseTier(minorOfNonMemberGuardian, nonMemberGuardian(), 204);
        getSummary(minorOfNonMemberGuardian, nonMemberGuardian())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.chooseTier").exists())
                .andExpect(jsonPath("$._templates.removeChoice").exists());

        removeChoice(minorOfNonMemberGuardian, nonMemberGuardian(), 204);
    }

    @Test
    @DisplayName("member chooses their own level")
    void memberChoosesOwnLevel() throws Exception {
        chooseTier(otherMember, loggedInAs(otherMember.getId().toUserId(), otherMember.getId()), 204);
    }

    @Test
    @DisplayName("guardian cannot choose or withdraw for a member who is not their minor")
    void guardianCannotChooseForUnrelatedMember() throws Exception {
        chooseTier(otherMember, memberGuardian(), 403);
        removeChoice(otherMember, memberGuardian(), 403);
        chooseTier(minor, nonMemberGuardian(), 403);
        removeChoice(minor, nonMemberGuardian(), 403);
    }

    @Test
    @DisplayName("unrelated member cannot choose or withdraw for another member")
    @WithKlabisMockUser(memberId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", authorities = {Authority.MEMBERS_READ})
    void unrelatedMemberCannotChoose() throws Exception {
        chooseTier(otherMember, CURRENT_USER, 403);
        removeChoice(otherMember, CURRENT_USER, 403);
    }

    @Test
    @DisplayName("administrator cannot choose or withdraw and sees no choice affordances")
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    void administratorCannotChooseAndSeesNoAffordances() throws Exception {
        feeYearLifecyclePort.chooseFeeLevel(new FeeYearLifecyclePort.ChooseFeeLevel(
                minor.getId(), new MembershipFeeGroupId(UUID.fromString(groupId)), YEAR));

        chooseTier(minor, CURRENT_USER, 403);
        removeChoice(minor, CURRENT_USER, 403);

        getSummary(minor, CURRENT_USER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentGroup.id").value(groupId))
                .andExpect(jsonPath("$._templates.chooseTier").doesNotExist())
                .andExpect(jsonPath("$._templates.removeChoice").doesNotExist());
        getChoice(minor, CURRENT_USER)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates.chooseTier").doesNotExist())
                .andExpect(jsonPath("$._templates.removeChoice").doesNotExist());
    }
}
