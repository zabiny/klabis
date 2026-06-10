package com.klabis.membershipfees.domain;

import com.klabis.finance.domain.Money;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record MembershipPaymentRule(
        EventTypeReference eventTypeId,
        String rankingShortName,
        RuleValue value
) {

    public MembershipPaymentRule {
        Assert.notNull(eventTypeId, "EventTypeId is required");
        Assert.hasText(rankingShortName, "RankingShortName is required");
        Assert.notNull(value, "RuleValue is required");
    }

    public static MembershipPaymentRule percentage(EventTypeReference eventTypeId, String rankingShortName, int percent) {
        return new MembershipPaymentRule(eventTypeId, rankingShortName, new RuleValue.Percentage(percent));
    }

    public static MembershipPaymentRule fixedAmount(EventTypeReference eventTypeId, String rankingShortName, Money amount) {
        Assert.notNull(amount, "Amount is required for fixed amount rule");
        return new MembershipPaymentRule(eventTypeId, rankingShortName, new RuleValue.FixedAmount(amount));
    }

    public boolean hasSameKey(MembershipPaymentRule other) {
        return this.eventTypeId.equals(other.eventTypeId)
                && this.rankingShortName.equals(other.rankingShortName);
    }

    public sealed interface RuleValue permits RuleValue.Percentage, RuleValue.FixedAmount {

        record Percentage(int percent) implements RuleValue {
            public Percentage {
                Assert.isTrue(percent >= 0, "Percentage must be non-negative");
            }
        }

        record FixedAmount(Money amount) implements RuleValue {
            public FixedAmount {
                Assert.notNull(amount, "Amount is required for fixed amount");
            }
        }
    }
}
