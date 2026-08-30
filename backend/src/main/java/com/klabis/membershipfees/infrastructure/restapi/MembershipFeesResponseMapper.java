package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.members.MemberDto;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.MemberFeeHistoryPort;
import com.klabis.membershipfees.domain.FeeGroupMembership;
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

    static PaymentRuleResponse toResponse(MembershipPaymentRule rule) {
        UUID eventTypeId = rule.eventTypeId().value();
        return switch (rule.value()) {
            case MembershipPaymentRule.RuleValue.Percentage p ->
                    PaymentRuleResponseBuilder.builder()
                            .eventTypeId(eventTypeId)
                            .rankingShortName(rule.rankingShortName())
                            .ruleType(PaymentRuleResponseRuleType.PERCENTAGE)
                            .percentage(p.percent())
                            .fixedAmount(null)
                            .fixedCurrency(null)
                            .build();
            case MembershipPaymentRule.RuleValue.FixedAmount f ->
                    PaymentRuleResponseBuilder.builder()
                            .eventTypeId(eventTypeId)
                            .rankingShortName(rule.rankingShortName())
                            .ruleType(PaymentRuleResponseRuleType.FIXED_AMOUNT)
                            .percentage(null)
                            .fixedAmount(f.amount().amount())
                            .fixedCurrency(f.amount().currency().getCurrencyCode())
                            .build();
        };
    }

    static MemberFeeChoiceResponse toResponse(UUID memberId, int year,
                                              Optional<MembershipFeeGroupId> currentChoice,
                                              Optional<MembershipFeeTierId> recommended) {
        return MemberFeeChoiceResponseBuilder.builder()
                .memberId(memberId)
                .year(year)
                .currentGroupId(currentChoice.map(MembershipFeeGroupId::value).orElse(null))
                .recommendedLevelId(recommended.map(MembershipFeeTierId::value).orElse(null))
                .build();
    }

    static MemberFeeHistoryResponse toResponse(List<MemberFeeHistoryPort.LevelAssignment> assignments) {
        return MemberFeeHistoryResponseBuilder.builder()
                .assignments(assignments.stream()
                        .map(a -> FeeAssignmentResponseBuilder.builder()
                                .year(a.year())
                                .groupId(a.groupId().value())
                                .groupName(a.groupName())
                                .joinedAt(a.joinedAt())
                                .source(MemberInGroupResponseSource.valueOf(a.source().name()))
                                .build())
                        .toList())
                .build();
    }

    static MemberFeeSummaryResponse toResponse(MemberFeeHistoryPort.CurrentLevelInfo info) {
        CurrentGroupResponse currentGroup = info.groupId() != null
                ? CurrentGroupResponseBuilder.builder()
                        .id(info.groupId().value())
                        .name(info.name())
                        .yearlyFee(info.yearlyFee().amount())
                        .build()
                : null;
        UUID recommendedLevelId = info.recommendedLevelId()
                .map(MembershipFeeTierId::value)
                .orElse(null);
        return MemberFeeSummaryResponseBuilder.builder()
                .currentGroup(currentGroup)
                .votingOpen(info.votingOpen())
                .recommendedLevelId(recommendedLevelId)
                .build();
    }

    static MemberInGroupResponse toResponse(FeeGroupMembership membership, @Nullable MemberDto memberDto) {
        return MemberInGroupResponseBuilder.builder()
                .memberId(membership.memberId().value())
                .firstName(memberDto != null ? memberDto.firstName() : null)
                .lastName(memberDto != null ? memberDto.lastName() : null)
                .registrationNumber(memberDto != null ? memberDto.registrationNumber() : null)
                .joinedAt(membership.joinedAt())
                .source(MemberInGroupResponseSource.valueOf(membership.source().name()))
                .build();
    }
}
