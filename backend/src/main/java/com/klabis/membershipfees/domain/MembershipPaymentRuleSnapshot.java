package com.klabis.membershipfees.domain;

import com.klabis.events.EventTypeId;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record MembershipPaymentRuleSnapshot(
        EventTypeId eventTypeId,
        String rankingShortName,
        MembershipPaymentRule.RuleValue value
) {

    public MembershipPaymentRuleSnapshot {
        Assert.notNull(eventTypeId, "EventTypeId is required");
        Assert.hasText(rankingShortName, "RankingShortName is required");
        Assert.notNull(value, "RuleValue is required");
    }

    static MembershipPaymentRuleSnapshot from(MembershipPaymentRule rule) {
        return new MembershipPaymentRuleSnapshot(rule.eventTypeId(), rule.rankingShortName(), rule.value());
    }
}
