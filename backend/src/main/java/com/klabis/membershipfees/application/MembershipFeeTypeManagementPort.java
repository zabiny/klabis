package com.klabis.membershipfees.application;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTypeId;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.MembershipFeeType;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

import java.util.List;

@PrimaryPort
public interface MembershipFeeTypeManagementPort {

    record CreateTypeCommand(String name, Money yearlyFee) {
        public CreateTypeCommand {
            Assert.hasText(name, "Name is required");
            Assert.notNull(yearlyFee, "YearlyFee is required");
        }
    }

    record EditTypeCommand(String name, Money yearlyFee, List<MembershipPaymentRule> rules) {
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

    record RemoveRuleCommand(EventTypeReference eventTypeId, String rankingShortName) {
        public RemoveRuleCommand {
            Assert.notNull(eventTypeId, "EventTypeId is required");
            Assert.hasText(rankingShortName, "RankingShortName is required");
        }
    }

    MembershipFeeTypeId createFeeType(CreateTypeCommand command);

    void editFeeType(MembershipFeeTypeId id, EditTypeCommand command);

    void addRule(MembershipFeeTypeId typeId, AddRuleCommand command);

    void editRule(MembershipFeeTypeId typeId, EditRuleCommand command);

    void removeRule(MembershipFeeTypeId typeId, RemoveRuleCommand command);

    MembershipFeeType getFeeType(MembershipFeeTypeId id);

    List<MembershipFeeType> listFeeTypes();

    void deleteFeeType(MembershipFeeTypeId id);
}
