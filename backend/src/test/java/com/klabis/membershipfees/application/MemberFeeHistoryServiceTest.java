package com.klabis.membershipfees.application;

import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberFeeHistoryService")
class MemberFeeHistoryServiceTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final MembershipFeeTierId LEVEL_ID = new MembershipFeeTierId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MembershipFeeGroupId GROUP_ID = new MembershipFeeGroupId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final int YEAR = 2026;

    @Mock
    private MembershipFeeGroupRepository groupRepository;
    @Mock
    private FeeSelectionCampaignRepository publicationRepository;
    @Mock
    private MemberChoicePort memberChoicePort;

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);
    private final Clock fixedClock = Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private MemberFeeHistoryService service;

    @BeforeEach
    void setUp() {
        service = new MemberFeeHistoryService(groupRepository, publicationRepository, memberChoicePort, fixedClock);
    }

    private static final LocalDate GROUP_DEADLINE = TODAY.plusDays(30);

    private MembershipFeeGroup buildGroupWithMember(MembershipFeeGroupId id, int year,
                                                     MemberId memberId, LocalDate joinedAt,
                                                     AssignmentSource source) {
        return MembershipFeeGroup.reconstruct(
                id, LEVEL_ID,
                "Základ", year, GROUP_DEADLINE,
                Money.ofCzk(new BigDecimal("500.00")),
                PublishedLevelStatus.FROZEN,
                List.of(),
                Set.of(new FeeGroupMembership(memberId, joinedAt, source, null)),
                null);
    }

    private FeeSelectionCampaign buildOpenPublication(int year) {
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(UUID.randomUUID()),
                year,
                TODAY.plusDays(30),
                null,
                List.of(GROUP_ID));
    }

    private FeeSelectionCampaign buildClosedPublication(int year) {
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(UUID.randomUUID()),
                year,
                TODAY.minusDays(1),
                null,
                List.of(GROUP_ID));
    }

    @Nested
    @DisplayName("getCurrentLevelInfo()")
    class GetCurrentLevelInfo {

        @Test
        @DisplayName("should return group info with yearlyFee when member has chosen a level")
        void shouldReturnGroupInfoWhenMemberHasChosen() {
            MembershipFeeGroup group = buildGroupWithMember(
                    GROUP_ID, YEAR, MEMBER_ID, LocalDate.of(YEAR, 1, 15), AssignmentSource.MEMBER_CHOICE);
            FeeSelectionCampaign publication = buildOpenPublication(YEAR);

            org.mockito.Mockito.when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR))
                    .thenReturn(Optional.of(group));
            org.mockito.Mockito.when(publicationRepository.findByYear(YEAR))
                    .thenReturn(Optional.of(publication));

            MemberFeeHistoryPort.CurrentLevelInfo result = service.getCurrentLevelInfo(MEMBER_ID, YEAR);

            assertThat(result.groupId()).isEqualTo(GROUP_ID);
            assertThat(result.name()).isEqualTo("Základ");
            assertThat(result.yearlyFee()).isEqualTo(Money.ofCzk(new BigDecimal("500.00")));
            assertThat(result.votingOpen()).isTrue();
            assertThat(result.recommendedLevelId()).isEmpty();
        }

        @Test
        @DisplayName("should return null groupId with votingOpen=true and recommendedLevelId when no choice and voting is open")
        void shouldReturnVotingOpenWithRecommendedWhenNoChoice() {
            FeeSelectionCampaign publication = buildOpenPublication(YEAR);

            org.mockito.Mockito.when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.when(publicationRepository.findByYear(YEAR))
                    .thenReturn(Optional.of(publication));
            org.mockito.Mockito.when(memberChoicePort.getRecommendedLevelForYear(MEMBER_ID, YEAR))
                    .thenReturn(Optional.of(LEVEL_ID));

            MemberFeeHistoryPort.CurrentLevelInfo result = service.getCurrentLevelInfo(MEMBER_ID, YEAR);

            assertThat(result.groupId()).isNull();
            assertThat(result.votingOpen()).isTrue();
            assertThat(result.recommendedLevelId()).contains(LEVEL_ID);
        }

        @Test
        @DisplayName("should return null groupId with votingOpen=false when no choice and voting is closed")
        void shouldReturnVotingClosedWhenNoChoiceAndClosed() {
            FeeSelectionCampaign publication = buildClosedPublication(YEAR);

            org.mockito.Mockito.when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.when(publicationRepository.findByYear(YEAR))
                    .thenReturn(Optional.of(publication));

            MemberFeeHistoryPort.CurrentLevelInfo result = service.getCurrentLevelInfo(MEMBER_ID, YEAR);

            assertThat(result.groupId()).isNull();
            assertThat(result.votingOpen()).isFalse();
        }

        @Test
        @DisplayName("should return null groupId with votingOpen=false when no publication for year")
        void shouldReturnNoPublicationForYear() {
            org.mockito.Mockito.when(groupRepository.findByMemberAndYear(MEMBER_ID, YEAR))
                    .thenReturn(Optional.empty());
            org.mockito.Mockito.when(publicationRepository.findByYear(YEAR))
                    .thenReturn(Optional.empty());

            MemberFeeHistoryPort.CurrentLevelInfo result = service.getCurrentLevelInfo(MEMBER_ID, YEAR);

            assertThat(result.groupId()).isNull();
            assertThat(result.votingOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("getLevelHistory()")
    class GetLevelHistory {

        @Test
        @DisplayName("should return assignment history sorted by year descending")
        void shouldReturnHistorySortedByYear() {
            LocalDate joined2024 = LocalDate.of(2024, 11, 1);
            LocalDate joined2025 = LocalDate.of(2025, 11, 1);

            MembershipFeeGroup group2024 = buildGroupWithMember(
                    new MembershipFeeGroupId(UUID.randomUUID()), 2024,
                    MEMBER_ID, joined2024, AssignmentSource.MEMBER_CHOICE);
            MembershipFeeGroup group2025 = buildGroupWithMember(
                    new MembershipFeeGroupId(UUID.randomUUID()), 2025,
                    MEMBER_ID, joined2025, AssignmentSource.ADMIN_ASSIGNMENT);

            org.mockito.Mockito.when(groupRepository.findByMember(MEMBER_ID))
                    .thenReturn(List.of(group2024, group2025));

            List<MemberFeeHistoryPort.LevelAssignment> history = service.getLevelHistory(MEMBER_ID);

            assertThat(history).hasSize(2);
            assertThat(history.get(0).year()).isEqualTo(2025);
            assertThat(history.get(0).joinedAt()).isEqualTo(joined2025);
            assertThat(history.get(0).source()).isEqualTo(AssignmentSource.ADMIN_ASSIGNMENT);
            assertThat(history.get(1).year()).isEqualTo(2024);
            assertThat(history.get(1).source()).isEqualTo(AssignmentSource.MEMBER_CHOICE);
        }

        @Test
        @DisplayName("should return empty list when member has no assignments")
        void shouldReturnEmptyWhenNoAssignments() {
            org.mockito.Mockito.when(groupRepository.findByMember(MEMBER_ID))
                    .thenReturn(List.of());

            List<MemberFeeHistoryPort.LevelAssignment> history = service.getLevelHistory(MEMBER_ID);

            assertThat(history).isEmpty();
        }
    }
}
