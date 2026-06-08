package com.klabis.membershipfees.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record MembershipPaymentRuleSnapshot(
        EventTypeReference eventTypeId,
        String rankingShortName,
        MembershipPaymentRule.RuleValue value
) {

    public MembershipPaymentRuleSnapshot {
        Assert.notNull(eventTypeId, "EventTypeId is required");
        Assert.hasText(rankingShortName, "RankingShortName is required");
        Assert.notNull(value, "RuleValue is required");
    }

    public static MembershipPaymentRuleSnapshot from(MembershipPaymentRule rule) {
        return new MembershipPaymentRuleSnapshot(rule.eventTypeId(), rule.rankingShortName(), rule.value());
    }
}
