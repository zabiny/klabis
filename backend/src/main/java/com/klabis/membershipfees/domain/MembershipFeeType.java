package com.klabis.membershipfees.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTypeId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@AggregateRoot
public class MembershipFeeType extends KlabisAggregateRoot<MembershipFeeType, MembershipFeeTypeId> {

    @Identity
    private final MembershipFeeTypeId id;
    private String name;
    private Money yearlyFee;
    private final List<MembershipPaymentRule> rules;

    private MembershipFeeType(MembershipFeeTypeId id, String name, Money yearlyFee,
                              List<MembershipPaymentRule> rules) {
        Assert.notNull(id, "MembershipFeeTypeId is required");
        Assert.hasText(name, "Name is required");
        Assert.notNull(yearlyFee, "YearlyFee is required");
        this.id = id;
        this.name = name;
        this.yearlyFee = yearlyFee;
        this.rules = new ArrayList<>(rules);
    }

    public static MembershipFeeType create(String name, Money yearlyFee) {
        MembershipFeeTypeId id = new MembershipFeeTypeId(UUID.randomUUID());
        return new MembershipFeeType(id, name, yearlyFee, List.of());
    }

    public static MembershipFeeType reconstruct(MembershipFeeTypeId id, String name, Money yearlyFee,
                                                List<MembershipPaymentRule> rules,
                                                AuditMetadata auditMetadata) {
        MembershipFeeType type = new MembershipFeeType(id, name, yearlyFee, rules);
        type.updateAuditMetadata(auditMetadata);
        return type;
    }

    @Override
    public MembershipFeeTypeId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Money getYearlyFee() {
        return yearlyFee;
    }

    public List<MembershipPaymentRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public void editName(String newName) {
        Assert.hasText(newName, "Name is required");
        this.name = newName;
    }

    public void editYearlyFee(Money newFee) {
        Assert.notNull(newFee, "YearlyFee is required");
        this.yearlyFee = newFee;
    }

    public void addRule(MembershipPaymentRule rule) {
        Assert.notNull(rule, "Rule is required");
        boolean duplicate = rules.stream().anyMatch(existing -> existing.hasSameKey(rule));
        if (duplicate) {
            throw new DuplicatePaymentRuleException(rule.eventTypeId(), rule.rankingShortName());
        }
        rules.add(rule);
    }

    public void editRuleValue(EventTypeReference eventTypeId, String rankingShortName, MembershipPaymentRule.RuleValue newValue) {
        Assert.notNull(eventTypeId, "EventTypeId is required");
        Assert.hasText(rankingShortName, "RankingShortName is required");
        Assert.notNull(newValue, "New rule value is required");
        int index = findRuleIndex(eventTypeId, rankingShortName);
        rules.set(index, new MembershipPaymentRule(eventTypeId, rankingShortName, newValue));
    }

    public void removeRule(EventTypeReference eventTypeId, String rankingShortName) {
        Assert.notNull(eventTypeId, "EventTypeId is required");
        Assert.hasText(rankingShortName, "RankingShortName is required");
        int index = findRuleIndex(eventTypeId, rankingShortName);
        rules.remove(index);
    }

    private int findRuleIndex(EventTypeReference eventTypeId, String rankingShortName) {
        for (int i = 0; i < rules.size(); i++) {
            MembershipPaymentRule rule = rules.get(i);
            if (rule.eventTypeId().equals(eventTypeId) && rule.rankingShortName().equals(rankingShortName)) {
                return i;
            }
        }
        throw new PaymentRuleNotFoundException(eventTypeId, rankingShortName);
    }

}
