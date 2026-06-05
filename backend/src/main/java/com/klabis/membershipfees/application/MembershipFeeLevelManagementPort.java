package com.klabis.membershipfees.application;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.MembershipFeeLevel;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

import java.util.List;

@PrimaryPort
public interface MembershipFeeLevelManagementPort {

    record CreateLevelCommand(String name, Money yearlyFee, List<MembershipPaymentRule> rules) {
        public CreateLevelCommand {
            Assert.hasText(name, "Name is required");
            Assert.notNull(yearlyFee, "YearlyFee is required");
            if (rules == null) {
                rules = List.of();
            }
        }
    }

    record EditLevelCommand(String name, Money yearlyFee, List<MembershipPaymentRule> rules) {
    }

    MembershipFeeLevelId createLevel(CreateLevelCommand command);

    void editLevel(MembershipFeeLevelId id, EditLevelCommand command);

    MembershipFeeLevel getLevel(MembershipFeeLevelId id);

    List<MembershipFeeLevel> listLevels();

    void deleteLevel(MembershipFeeLevelId id);
}
