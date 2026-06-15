package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.MembershipFeeTier;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Table(schema = "membershipfees", value = "membership_fee_tier")
class MembershipFeeTierMemento extends AbstractMembershipFeeMemento {

    @Column("name")
    private String name;

    @Column("yearly_fee_amount")
    private BigDecimal yearlyFeeAmount;

    @Column("yearly_fee_currency")
    private String yearlyFeeCurrency;

    @MappedCollection(idColumn = "membership_fee_tier_id")
    private Set<MembershipPaymentRuleMemento> rules = new HashSet<>();

    protected MembershipFeeTierMemento() {
    }

    static MembershipFeeTierMemento from(MembershipFeeTier level) {
        MembershipFeeTierMemento memento = new MembershipFeeTierMemento();
        memento.id = level.getId().value();
        memento.name = level.getName();
        memento.yearlyFeeAmount = level.getYearlyFee().amount();
        memento.yearlyFeeCurrency = level.getYearlyFee().currency().getCurrencyCode();
        memento.rules = level.getRules().stream()
                .map(MembershipPaymentRuleMemento::from)
                .collect(Collectors.toSet());
        memento.isNew = (level.getAuditMetadata() == null);
        memento.applyAudit(level.getAuditMetadata());
        return memento;
    }

    MembershipFeeTier toTier() {
        List<MembershipPaymentRule> domainRules = rules.stream()
                .map(MembershipPaymentRuleMemento::toRule)
                .toList();

        Money yearlyFee = Money.of(yearlyFeeAmount, Currency.getInstance(yearlyFeeCurrency));

        return MembershipFeeTier.reconstruct(new MembershipFeeTierId(id), name, yearlyFee,
                domainRules, toAuditMetadata());
    }
}
