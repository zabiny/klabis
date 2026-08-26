package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.members.MemberDto;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.MemberFeeHistoryPort;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeTier;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Response DTOs are generated from the spec (see build.gradle.kts openApiModule("membershipfees")),
// so the domain -> response mapping that used to live as static factory methods on the
// hand-written DTOs now lives here.
final class MembershipFeesResponseMapper {

    private MembershipFeesResponseMapper() {
    }

    static FeeSelectionCampaignResponse toResponse(FeeSelectionCampaign publication) {
        return new FeeSelectionCampaignResponse(
                publication.getDeadlineProcessedAt(),
                publication.getId().value(),
                publication.getVotingDeadline(),
                publication.getYear()
        );
    }

    static MembershipFeeTierResponse toResponse(MembershipFeeTier level) {
        return new MembershipFeeTierResponse(
                level.getId().value(),
                level.getName(),
                level.getYearlyFee().amount(),
                level.getYearlyFee().currency().getCurrencyCode()
        );
    }

    static MembershipFeeTierSummaryResponse toSummaryResponse(MembershipFeeTier level) {
        return new MembershipFeeTierSummaryResponse(
                level.getId().value(),
                level.getName(),
                level.getRules().size(),
                level.getYearlyFee().amount(),
                level.getYearlyFee().currency().getCurrencyCode()
        );
    }

    static PaymentRuleResponse toResponse(MembershipPaymentRule rule) {
        UUID eventTypeId = rule.eventTypeId().value();
        return switch (rule.value()) {
            case MembershipPaymentRule.RuleValue.Percentage p ->
                    new PaymentRuleResponse(eventTypeId, null, null, p.percent(),
                            rule.rankingShortName(), PaymentRuleResponseRuleType.PERCENTAGE);
            case MembershipPaymentRule.RuleValue.FixedAmount f ->
                    new PaymentRuleResponse(eventTypeId, f.amount().amount(),
                            f.amount().currency().getCurrencyCode(), null,
                            rule.rankingShortName(), PaymentRuleResponseRuleType.FIXED_AMOUNT);
        };
    }

    static MemberFeeChoiceResponse toResponse(UUID memberId, int year,
                                              Optional<MembershipFeeGroupId> currentChoice,
                                              Optional<MembershipFeeTierId> recommended) {
        return new MemberFeeChoiceResponse(
                currentChoice.map(MembershipFeeGroupId::value).orElse(null),
                memberId,
                recommended.map(MembershipFeeTierId::value).orElse(null),
                year);
    }

    static MemberFeeHistoryResponse toResponse(List<MemberFeeHistoryPort.LevelAssignment> assignments) {
        return new MemberFeeHistoryResponse(
                assignments.stream()
                        .map(a -> new FeeAssignmentResponse(
                                a.groupId().value(),
                                a.groupName(),
                                a.joinedAt(),
                                FeeAssignmentResponseSource.valueOf(a.source().name()),
                                a.year()))
                        .toList());
    }

    static MemberFeeSummaryResponse toResponse(MemberFeeHistoryPort.CurrentLevelInfo info) {
        CurrentGroupResponse currentGroup = info.groupId() != null
                ? new CurrentGroupResponse(
                        info.groupId().value(),
                        info.name(),
                        info.yearlyFee().amount())
                : null;
        UUID recommendedLevelId = info.recommendedLevelId()
                .map(MembershipFeeTierId::value)
                .orElse(null);
        return new MemberFeeSummaryResponse(currentGroup, recommendedLevelId, info.votingOpen());
    }

    static MembershipFeeGroupResponse toResponse(MembershipFeeGroup group) {
        return new MembershipFeeGroupResponse(
                group.getId().value(),
                group.memberCount(),
                group.getName(),
                group.getRulesSnapshot().stream().map(MembershipFeesResponseMapper::toResponse).toList(),
                group.getSourceLevelId().value(),
                MembershipFeeGroupResponseStatus.valueOf(group.getStatus().name()),
                group.getYear(),
                group.getYearlyFeeSnapshot().amount(),
                group.getYearlyFeeSnapshot().currency().getCurrencyCode()
        );
    }

    static MemberInGroupResponse toResponse(FeeGroupMembership membership, @Nullable MemberDto memberDto) {
        return new MemberInGroupResponse(
                memberDto != null ? memberDto.firstName() : null,
                membership.joinedAt(),
                memberDto != null ? memberDto.lastName() : null,
                membership.memberId().value(),
                memberDto != null ? memberDto.registrationNumber() : null,
                FeeAssignmentResponseSource.valueOf(membership.source().name())
        );
    }
}
