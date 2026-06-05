package com.klabis.membershipfees.domain;

import com.klabis.events.EventTypeId;
import com.klabis.finance.domain.Money;
import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

@ValueObject
public record MembershipPaymentRule(
        EventTypeId eventTypeId,
        String rankingShortName,
        RuleValue value
) {

    public MembershipPaymentRule {
        Assert.notNull(eventTypeId, "EventTypeId is required");
        Assert.hasText(rankingShortName, "RankingShortName is required");
        Assert.notNull(value, "RuleValue is required");
    }

    public static MembershipPaymentRule percentage(EventTypeId eventTypeId, String rankingShortName, int percent) {
        return new MembershipPaymentRule(eventTypeId, rankingShortName, new RuleValue.Percentage(percent));
    }

    public static MembershipPaymentRule fixedSurcharge(EventTypeId eventTypeId, String rankingShortName, Money amount) {
        Assert.notNull(amount, "Amount is required for fixed surcharge rule");
        return new MembershipPaymentRule(eventTypeId, rankingShortName, new RuleValue.FixedSurcharge(amount));
    }

    public boolean hasSameKey(MembershipPaymentRule other) {
        return this.eventTypeId.equals(other.eventTypeId)
                && this.rankingShortName.equals(other.rankingShortName);
    }

    public sealed interface RuleValue permits RuleValue.Percentage, RuleValue.FixedSurcharge {

        record Percentage(int percent) implements RuleValue {
            public Percentage {
                Assert.isTrue(percent >= 0, "Percentage must be non-negative");
            }
        }

        record FixedSurcharge(Money amount) implements RuleValue {
            public FixedSurcharge {
                Assert.notNull(amount, "Amount is required for fixed surcharge");
            }
        }
    }
}
