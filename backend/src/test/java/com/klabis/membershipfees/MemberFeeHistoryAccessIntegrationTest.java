package com.klabis.membershipfees;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.finance.domain.Money;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
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

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RegistrationPort registrationPort;
    @Autowired
    private MembershipFeeTierManagementPort tierManagementPort;
    @Autowired
    private MembershipFeeGroupRepository groupRepository;

    private Member member;

    @BeforeEach
    void memberWithPastAssignment() {
        member = registrationPort.registerMember(new RegistrationPort.RegisterNewMember(
                PersonalInformation.of("Jan", "Novák", LocalDate.of(1990, 1, 1), "CZ", Gender.MALE),
                Address.of("Hlavní 1", "Praha", "11000", "CZ"),
                EmailAddress.of("jan.novak." + UUID.randomUUID() + "@example.com"),
                PhoneNumber.of("+420777111222"),
                BirthNumber.of("900101/1234"),
                null, null, List.of(), null));

        var tierId = tierManagementPort.createTier(
                new MembershipFeeTierManagementPort.CreateTierCommand("Závodní", YEARLY_FEE));
        MembershipFeeGroup group = MembershipFeeGroup.createSnapshot(
                tierId, "Závodní", PAST_YEAR, YEARLY_FEE, List.of(), LocalDate.of(PAST_YEAR - 1, 12, 31));
        group.addMember(member.getId(), LocalDate.of(PAST_YEAR - 1, 11, 20), AssignmentSource.ADMIN_ASSIGNMENT);
        groupRepository.save(group);
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
                .andExpect(jsonPath("$._links.feeSummary").doesNotExist());
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
