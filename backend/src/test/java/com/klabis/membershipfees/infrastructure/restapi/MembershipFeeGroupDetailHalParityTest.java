package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.HalShape;
import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.finance.domain.Money;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.MembershipFeesWebMvcTest;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Membership fee group detail HAL parity (_embedded.members)")
@MembershipFeesWebMvcTest
class MembershipFeeGroupDetailHalParityTest {

    private static final UUID GROUP_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MEMBER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID TIER_UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final String GROUP = "/api/membership-fee-groups/" + GROUP_UUID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeeSelectionCampaignManagementPort managementPort;

    @Autowired
    private Members members;

    private String getGroupBody() throws Exception {
        FeeGroupMembership membership = new FeeGroupMembership(new MemberId(MEMBER_UUID), LocalDate.of(2026, 1, 15),
                AssignmentSource.MEMBER_CHOICE, null);
        MembershipFeeGroup group = MembershipFeeGroup.reconstruct(new MembershipFeeGroupId(GROUP_UUID),
                new MembershipFeeTierId(TIER_UUID), "Dospělý", 2026, LocalDate.of(2026, 3, 31),
                Money.ofCzk(new BigDecimal("1200.00")), PublishedLevelStatus.FROZEN, List.of(), Set.of(membership), null);
        when(managementPort.getGroup(new MembershipFeeGroupId(GROUP_UUID))).thenReturn(group);
        when(members.findByIds(any())).thenReturn(Map.of(new MemberId(MEMBER_UUID),
                new MemberDto(MEMBER_UUID, "Jan", "Novák", "jan@example.com", "ZBM1234", LocalDateTime.of(2025, 1, 1, 0, 0))));
        return mockMvc.perform(get("/api/membership-fee-groups/{id}", GROUP_UUID).accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @DisplayName("members port is not called when a plain JSON response is rendered")
    @WithKlabisMockUser(memberId = "00000000-0000-0000-0000-000000000001", authorities = {Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ})
    void plainJsonDoesNotLoadMembers() throws Exception {
        FeeGroupMembership membership = new FeeGroupMembership(new MemberId(MEMBER_UUID), LocalDate.of(2026, 1, 15),
                AssignmentSource.MEMBER_CHOICE, null);
        MembershipFeeGroup group = MembershipFeeGroup.reconstruct(new MembershipFeeGroupId(GROUP_UUID),
                new MembershipFeeTierId(TIER_UUID), "Dospělý", 2026, LocalDate.of(2026, 3, 31),
                Money.ofCzk(new BigDecimal("1200.00")), PublishedLevelStatus.FROZEN, List.of(), Set.of(membership), null);
        when(managementPort.getGroup(new MembershipFeeGroupId(GROUP_UUID))).thenReturn(group);

        mockMvc.perform(get("/api/membership-fee-groups/{id}", GROUP_UUID).accept("application/json"))
                .andExpect(status().isOk());

        verifyNoInteractions(members);
    }

    @Test
    @DisplayName("members port is called exactly once when a HAL response is rendered")
    @WithKlabisMockUser(memberId = "00000000-0000-0000-0000-000000000001", authorities = {Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ})
    void halLoadsMembersOnce() throws Exception {
        getGroupBody();

        verify(members, times(1)).findByIds(any());
    }

    @Test
    @DisplayName("admin: group links/templates and embedded member rows")
    @WithKlabisMockUser(memberId = "00000000-0000-0000-0000-000000000001", authorities = {Authority.MEMBERS_MANAGE, Authority.MEMBERS_READ})
    void adminView() throws Exception {
        String body = getGroupBody();

        assertThat(HalShape.of(body, "$")).isEqualTo(List.of("link rules -> " + GROUP + "/rules", "link self -> " + GROUP, "link sourceLevel -> /api/membership-fee-tiers/" + TIER_UUID, "template assignMember POST " + GROUP + "/members [memberId, year]"));
        assertThat(HalShape.of(body, "$._embedded.members[0]")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("plain user: group links/templates and embedded member rows")
    @WithKlabisMockUser(memberId = "00000000-0000-0000-0000-000000000002")
    void plainUserView() throws Exception {
        String body = getGroupBody();

        assertThat(HalShape.of(body, "$")).isEqualTo(List.of("link rules -> " + GROUP + "/rules", "link self -> " + GROUP, "link sourceLevel -> /api/membership-fee-tiers/" + TIER_UUID));
        assertThat(HalShape.of(body, "$._embedded.members[0]")).isEqualTo(List.of());
    }
}
