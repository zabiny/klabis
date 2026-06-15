package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.members.MemberDto;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

record MembershipFeeGroupResponse(
        UUID id,
        UUID sourceLevelId,
        String name,
        int year,
        BigDecimal yearlyFeeAmount,
        String yearlyFeeCurrency,
        String status,
        int memberCount,
        List<MembershipFeeTierResponse.PaymentRuleResponse> rulesSnapshot
) {
    static MembershipFeeGroupResponse from(MembershipFeeGroup group) {
        return new MembershipFeeGroupResponse(
                group.getId().value(),
                group.getSourceLevelId().value(),
                group.getName(),
                group.getYear(),
                group.getYearlyFeeSnapshot().amount(),
                group.getYearlyFeeSnapshot().currency().getCurrencyCode(),
                group.getStatus().name(),
                group.memberCount(),
                group.getRulesSnapshot().stream().map(MembershipFeeTierResponse.PaymentRuleResponse::from).toList()
        );
    }

    @Relation(collectionRelation = "members")
    record MemberInGroupResponse(
            UUID memberId,
            @Nullable String firstName,
            @Nullable String lastName,
            @Nullable String registrationNumber,
            LocalDate joinedAt,
            AssignmentSource source
    ) {
        static MemberInGroupResponse from(FeeGroupMembership membership, @Nullable MemberDto memberDto) {
            return new MemberInGroupResponse(
                    membership.memberId().value(),
                    memberDto != null ? memberDto.firstName() : null,
                    memberDto != null ? memberDto.lastName() : null,
                    memberDto != null ? memberDto.registrationNumber() : null,
                    membership.joinedAt(),
                    membership.source()
            );
        }
    }
}
