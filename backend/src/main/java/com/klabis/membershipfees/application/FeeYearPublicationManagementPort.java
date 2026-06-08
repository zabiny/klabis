package com.klabis.membershipfees.application;

import com.klabis.membershipfees.FeeYearPublicationId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.FeeYearPublication;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@PrimaryPort
public interface FeeYearPublicationManagementPort {

    record PublishYearCommand(int year, LocalDate votingDeadline, List<MembershipFeeLevelId> levelIds) {
        public PublishYearCommand {
            Assert.notNull(votingDeadline, "VotingDeadline is required");
            Assert.notEmpty(levelIds, "At least one level ID is required");
        }
    }

    FeeYearPublicationId publishYear(PublishYearCommand command);

    FeeYearPublication getPublication(FeeYearPublicationId id);

    Optional<FeeYearPublication> getPublicationForYear(int year);

    List<FeeYearPublication> listPublications();

    List<MembershipFeeGroup> listGroupsForYear(int year);

    MembershipFeeGroup getGroup(MembershipFeeGroupId id);
}
