package com.klabis.membershipfees.application;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTypeId;
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

    record PublishYearCommand(int year, LocalDate votingDeadline, List<MembershipFeeTypeId> typeIds) {
        public PublishYearCommand {
            Assert.notNull(votingDeadline, "VotingDeadline is required");
            Assert.notEmpty(typeIds, "At least one type ID is required");
        }
    }

    record ChangeDeadlineCommand(LocalDate votingDeadline) {
        public ChangeDeadlineCommand {
            Assert.notNull(votingDeadline, "VotingDeadline is required");
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

    List<FeeSelectionCampaign> listPublications(CampaignStatusFilter filter);

    Optional<FeeSelectionCampaign> findActiveCampaign();

    List<MembershipFeeGroup> listGroupsForYear(int year);

    MembershipFeeGroup getGroup(MembershipFeeGroupId id);

    void editGroupSnapshot(MembershipFeeGroupId id, EditGroupSnapshotCommand command);

    FeeSelectionCampaign changeDeadline(FeeSelectionCampaignId id, ChangeDeadlineCommand command);

    /**
     * Returns the year of the active campaign if one exists, otherwise the latest campaign year
     * that is not in the past (so a just-closed campaign still counts), otherwise the current year.
     * Used to build links pointing to the most relevant fee summary endpoint.
     */
    int relevantFeeYear(LocalDate today);
}
