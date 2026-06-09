package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

@Table(schema = "membershipfees", value = "membership_payment_rule")
class MembershipPaymentRuleMemento {

    @Id
    @Column("id")
    private UUID id;

    @Column("event_type_id")
    private UUID eventTypeId;

    @Column("ranking_short_name")
    private String rankingShortName;

    @Column("rule_type")
    private String ruleType;

    @Column("rule_percentage")
    private Integer rulePercentage;

    @Column("rule_fixed_amount")
    private BigDecimal ruleFixedAmount;

    @Column("rule_fixed_currency")
    private String ruleFixedCurrency;

    protected MembershipPaymentRuleMemento() {
    }

    static MembershipPaymentRuleMemento from(MembershipPaymentRule rule) {
        MembershipPaymentRuleMemento memento = new MembershipPaymentRuleMemento();
        memento.id = UUID.randomUUID();
        memento.eventTypeId = rule.eventTypeId().value();
        memento.rankingShortName = rule.rankingShortName();

        switch (rule.value()) {
            case MembershipPaymentRule.RuleValue.Percentage p -> {
                memento.ruleType = "PERCENTAGE";
                memento.rulePercentage = p.percent();
            }
            case MembershipPaymentRule.RuleValue.FixedSurcharge f -> {
                memento.ruleType = "FIXED_SURCHARGE";
                memento.ruleFixedAmount = f.amount().amount();
                memento.ruleFixedCurrency = f.amount().currency().getCurrencyCode();
            }
        }
        return memento;
    }

    MembershipPaymentRule toRule() {
        MembershipPaymentRule.RuleValue value = switch (ruleType) {
            case "PERCENTAGE" -> new MembershipPaymentRule.RuleValue.Percentage(rulePercentage);
            case "FIXED_SURCHARGE" -> new MembershipPaymentRule.RuleValue.FixedSurcharge(
                    Money.of(ruleFixedAmount, Currency.getInstance(ruleFixedCurrency)));
            default -> throw new IllegalStateException("Unknown rule type: " + ruleType);
        };
        return new MembershipPaymentRule(EventTypeReference.of(eventTypeId), rankingShortName, value);
    }
}
