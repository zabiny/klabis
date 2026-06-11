package com.klabis.membershipfees.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTierId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@AggregateRoot
public class MembershipFeeTier extends KlabisAggregateRoot<MembershipFeeTier, MembershipFeeTierId> {

    @Identity
    private final MembershipFeeTierId id;
    private String name;
    private Money yearlyFee;
    private final List<MembershipPaymentRule> rules;

    private MembershipFeeTier(MembershipFeeTierId id, String name, Money yearlyFee,
                              List<MembershipPaymentRule> rules) {
        Assert.notNull(id, "MembershipFeeTierId is required");
        Assert.hasText(name, "Name is required");
        Assert.notNull(yearlyFee, "YearlyFee is required");
        this.id = id;
        this.name = name;
        this.yearlyFee = yearlyFee;
        this.rules = new ArrayList<>(rules);
    }

    public static MembershipFeeTier create(String name, Money yearlyFee) {
        MembershipFeeTierId id = new MembershipFeeTierId(UUID.randomUUID());
        return new MembershipFeeTier(id, name, yearlyFee, List.of());
    }

    public static MembershipFeeTier reconstruct(MembershipFeeTierId id, String name, Money yearlyFee,
                                                List<MembershipPaymentRule> rules,
                                                AuditMetadata auditMetadata) {
        MembershipFeeTier level = new MembershipFeeTier(id, name, yearlyFee, rules);
        level.updateAuditMetadata(auditMetadata);
        return level;
    }

    @Override
    public MembershipFeeTierId getId() {
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
