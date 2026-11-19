package com.klabis.membershipfees;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.authorization.AuthorizationSnapshotLoader;
import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.finance.domain.Money;
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
import com.klabis.membershipfees.application.MembershipFeeTypeManagementPort;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Member fee history access — full-stack integration")
class MemberFeeHistoryAccessIntegrationTest {

    private static final String ADMIN_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String OTHER_MEMBER_UUID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final Money YEARLY_FEE = Money.of(new BigDecimal("1200.00"), Currency.getInstance("CZK"));
    private static final int PAST_YEAR = 2024;
    private static final int CURRENT_YEAR = LocalDate.now().getYear();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private MembershipFeeTypeManagementPort typeManagementPort;
    @Autowired
    private MembershipFeeGroupRepository groupRepository;
    @Autowired
    private AuthorizationSnapshotLoader snapshotLoader;
    @Autowired
    private LegalGuardianGroupPort legalGuardianGroupPort;

    private static final RequestPostProcessor CURRENT_USER = request -> request;

    private Member member;
    private MembershipFeeGroup group;

    @BeforeEach
    void memberWithPastAssignment() {
        member = registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", "Novák", LocalDate.of(1990, 1, 1), Nationality.of("CZ"), Gender.MALE),
                Address.of("Hlavní 1", "Praha", "11000", Country.of("CZ")),
                EmailAddress.of("jan.novak." + UUID.randomUUID() + "@example.com"),
                PhoneNumber.of("+420777111222"),
                BirthNumber.of("900101/1234"),
                null, null, List.of(), null));

        var typeId = typeManagementPort.createFeeType(
                new MembershipFeeTypeManagementPort.CreateTypeCommand("Závodní", YEARLY_FEE));
        group = MembershipFeeGroup.createSnapshot(
                typeId, "Závodní", PAST_YEAR, YEARLY_FEE, List.of(), LocalDate.of(PAST_YEAR - 1, 12, 31));
        group.addMember(member.getId(), LocalDate.of(PAST_YEAR - 1, 11, 20), AssignmentSource.ADMIN_ASSIGNMENT);
        group = groupRepository.save(group);
    }

    private Member registerMinor(List<LegalGuardianPort.GuardianInput> guardians) {
        Member minor = registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Sofie", "Svobodová", LocalDate.now().minusYears(12), Nationality.of("CZ"), Gender.FEMALE),
                Address.of("Dětská 1", "Praha", "11000", Country.of("CZ")),
                null, null, BirthNumber.of("900101/1234"), null, null, guardians, null));
        group.addMember(minor.getId(), LocalDate.of(PAST_YEAR - 1, 11, 20), AssignmentSource.ADMIN_ASSIGNMENT);
        group = groupRepository.save(group);
        return minor;
    }

    private RequestPostProcessor loggedInAs(UserId userId, MemberId memberId) {
        JwtParams params = JwtParams.jwtTokenParams("guardian", userId).withMemberId(memberId);
        return authentication(KlabisAuthenticationFactory.createAuthenticationToken(params,
                snapshotLoader.loadFor(userId)));
    }

    private void assertGuardianFollowsFeeHistoryOfMinorOnly(RequestPostProcessor guardian, Member minor)
            throws Exception {
        mockMvc.perform(get("/api/members/{id}", minor.getId().uuid()).with(guardian)
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeHistory.href").value(
                        endsWith("/api/members/" + minor.getId().uuid() + "/fee-history")));

        mockMvc.perform(get("/api/members/{id}/fee-history", minor.getId().uuid()).with(guardian)
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments[0].year").value(PAST_YEAR))
                .andExpect(jsonPath("$.assignments[0].groupName").value("Závodní"))
                .andExpect(jsonPath("$._links.feeSummary.href").exists());

        mockMvc.perform(get("/api/members/{id}/fee-history", member.getId().uuid()).with(guardian)
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isForbidden());

        assertGuardianReadsCurrentLevelOfMinorOnly(guardian, minor);
    }

    private ResultActions getSummary(Member target, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}/fee-summary/{year}", target.getId().uuid(), CURRENT_YEAR)
                .with(user).accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private ResultActions getChoice(Member target, RequestPostProcessor user) throws Exception {
        return mockMvc.perform(get("/api/members/{id}/fee-choice/{year}", target.getId().uuid(), CURRENT_YEAR)
                .with(user).accept(MediaTypes.HAL_FORMS_JSON_VALUE));
    }

    private void assertGuardianReadsCurrentLevelOfMinorOnly(RequestPostProcessor guardian, Member minor)
            throws Exception {
        getSummary(minor, guardian)
                .andExpect(status().isOk());
        getChoice(minor, guardian)
                .andExpect(status().isOk());

        getSummary(member, guardian)
                .andExpect(status().isForbidden());
        getChoice(member, guardian)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("member guardian follows feeHistory of their minor and is forbidden for another member")
    void memberGuardianFollowsFeeHistoryOfMinor() throws Exception {
        Member guardian = registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Eva", "Svobodová", LocalDate.of(1985, 5, 5), Nationality.of("CZ"), Gender.FEMALE),
                Address.of("Hlavní 2", "Praha", "11000", Country.of("CZ")),
                EmailAddress.of("eva." + UUID.randomUUID() + "@example.com"),
                PhoneNumber.of("+420777111333"),
                BirthNumber.of("855505/1234"),
                null, null, List.of(), null));
        Member minor = registerMinor(List.of(LegalGuardianPort.GuardianInput.existing(guardian.getId().toUserId())));

        assertGuardianFollowsFeeHistoryOfMinorOnly(
                loggedInAs(guardian.getId().toUserId(), guardian.getId()), minor);
    }

    @Test
    @DisplayName("non-member guardian follows feeHistory of their minor and is forbidden for another member")
    void nonMemberGuardianFollowsFeeHistoryOfMinor() throws Exception {
        Member minor = registerMinor(List.of(LegalGuardianPort.GuardianInput.created(
                new LegalGuardianPort.NewLegalGuardian("Ivana", "Dlouhá", "ivana." + UUID.randomUUID() + "@example.com",
                        "+420604100100"))));
        UserId guardianId = legalGuardianGroupPort.guardiansOf(minor.getId()).iterator().next();

        assertGuardianFollowsFeeHistoryOfMinorOnly(loggedInAs(guardianId, null), minor);
    }

    @Test
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    @DisplayName("administrator sees feeHistory on the member detail and follows it to the assignment")
    void administratorFollowsFeeHistoryLink() throws Exception {
        mockMvc.perform(get("/api/members/{id}", member.getId().uuid()).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.feeHistory.href").value(
                        endsWith("/api/members/" + member.getId().uuid() + "/fee-history")));

        mockMvc.perform(get("/api/members/{id}/fee-history", member.getId().uuid())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments.length()").value(1))
                .andExpect(jsonPath("$.assignments[0].year").value(PAST_YEAR))
                .andExpect(jsonPath("$.assignments[0].groupId").exists())
                .andExpect(jsonPath("$.assignments[0].groupName").value("Závodní"))
                .andExpect(jsonPath("$.assignments[0].source").value("ADMIN_ASSIGNMENT"))
                .andExpect(jsonPath("$._links.member.href").exists())
                .andExpect(jsonPath("$._links.feeSummary.href").exists());
    }

    @Test
    @WithKlabisMockUser(memberId = ADMIN_UUID, authorities = {Authority.MEMBERS_READ, Authority.MEMBERS_MANAGE})
    @DisplayName("administrator reads the current level of a member")
    void administratorReadsCurrentLevel() throws Exception {
        getSummary(member, CURRENT_USER)
                .andExpect(status().isOk());
        getChoice(member, CURRENT_USER)
                .andExpect(status().isOk());
    }

    @Test
    @WithKlabisMockUser(memberId = OTHER_MEMBER_UUID, authorities = {Authority.MEMBERS_READ})
    @DisplayName("ordinary member is forbidden to read another member's current level")
    void ordinaryMemberIsForbiddenToReadCurrentLevel() throws Exception {
        getSummary(member, CURRENT_USER)
                .andExpect(status().isForbidden());
        getChoice(member, CURRENT_USER)
                .andExpect(status().isForbidden());
    }

    @Test
    @WithKlabisMockUser(memberId = OTHER_MEMBER_UUID, authorities = {Authority.MEMBERS_READ})
    @DisplayName("ordinary member is forbidden to read another member's history")
    void ordinaryMemberIsForbidden() throws Exception {
        mockMvc.perform(get("/api/members/{id}/fee-history", member.getId().uuid())
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isForbidden());
    }
}
