package com.klabis.membershipfees.application;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.MembershipFeeTier;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

import java.util.List;

@PrimaryPort
public interface MembershipFeeTierManagementPort {

    record CreateTierCommand(String name, Money yearlyFee) {
        public CreateTierCommand {
            Assert.hasText(name, "Name is required");
            Assert.notNull(yearlyFee, "YearlyFee is required");
        }
    }

    record EditTierCommand(String name, Money yearlyFee, List<MembershipPaymentRule> rules) {
    }

    record AddRuleCommand(MembershipPaymentRule rule) {
        public AddRuleCommand {
            Assert.notNull(rule, "Rule is required");
        }
    }

    record EditRuleCommand(EventTypeReference eventTypeId, String rankingShortName, MembershipPaymentRule.RuleValue newValue) {
        public EditRuleCommand {
            Assert.notNull(eventTypeId, "EventTypeId is required");
            Assert.hasText(rankingShortName, "RankingShortName is required");
            Assert.notNull(newValue, "New rule value is required");
        }
    }

    MembershipFeeTierId createTier(CreateTierCommand command);

    void editTier(MembershipFeeTierId id, EditTierCommand command);

    void addRule(MembershipFeeTierId tierId, AddRuleCommand command);

    void editRule(MembershipFeeTierId tierId, EditRuleCommand command);

    MembershipFeeTier getTier(MembershipFeeTierId id);

    List<MembershipFeeTier> listTiers();

    void deleteTier(MembershipFeeTierId id);
}
