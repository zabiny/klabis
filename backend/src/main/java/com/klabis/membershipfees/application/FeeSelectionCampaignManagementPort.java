package com.klabis.membershipfees.application;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@PrimaryPort
public interface FeeSelectionCampaignManagementPort {

    record PublishYearCommand(int year, LocalDate votingDeadline, List<MembershipFeeTierId> levelIds) {
        public PublishYearCommand {
            Assert.notNull(votingDeadline, "VotingDeadline is required");
            Assert.notEmpty(levelIds, "At least one level ID is required");
        }
    }

    record EditGroupSnapshotCommand(Money yearlyFee, List<MembershipPaymentRule> rules) {
        public EditGroupSnapshotCommand {
            Assert.notNull(yearlyFee, "YearlyFee is required");
            if (rules == null) {
                rules = List.of();
            }
        }
    }

    FeeSelectionCampaignId publishYear(PublishYearCommand command);

    FeeSelectionCampaign getPublication(FeeSelectionCampaignId id);

    Optional<FeeSelectionCampaign> getPublicationForYear(int year);

    List<FeeSelectionCampaign> listPublications();

    List<MembershipFeeGroup> listGroupsForYear(int year);

    MembershipFeeGroup getGroup(MembershipFeeGroupId id);

    void editGroupSnapshot(MembershipFeeGroupId id, EditGroupSnapshotCommand command);
}
