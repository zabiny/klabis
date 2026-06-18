package com.klabis.membershipfees.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@AggregateRoot
public class FeeSelectionCampaign extends KlabisAggregateRoot<FeeSelectionCampaign, FeeSelectionCampaignId> {

    @Identity
    private final FeeSelectionCampaignId id;
    private final int year;
    private LocalDate votingDeadline;
    @Nullable
    private Instant deadlineProcessedAt;
    private final List<MembershipFeeGroupId> publishedGroupIds;

    private FeeSelectionCampaign(FeeSelectionCampaignId id, int year, LocalDate votingDeadline,
                                 @Nullable Instant deadlineProcessedAt,
                                 List<MembershipFeeGroupId> publishedGroupIds) {
        Assert.notNull(id, "FeeSelectionCampaignId is required");
        Assert.notNull(votingDeadline, "VotingDeadline is required");
        this.id = id;
        this.year = year;
        this.votingDeadline = votingDeadline;
        this.deadlineProcessedAt = deadlineProcessedAt;
        this.publishedGroupIds = new ArrayList<>(publishedGroupIds);
    }

    public static FeeSelectionCampaignWithGroups publish(int year, LocalDate votingDeadline,
                                                         List<MembershipFeeTier> levels) {
        Assert.notNull(votingDeadline, "VotingDeadline is required");
        Assert.notEmpty(levels, "At least one level is required for publishing");

        List<MembershipFeeGroup> groups = levels.stream()
                .map(level -> MembershipFeeGroup.createSnapshot(
                        level.getId(),
                        level.getName(),
                        year,
                        level.getYearlyFee(),
                        level.getRules(),
                        votingDeadline))
                .toList();

        List<MembershipFeeGroupId> groupIds = groups.stream()
                .map(MembershipFeeGroup::getId)
                .toList();

        FeeSelectionCampaign publication = new FeeSelectionCampaign(
                new FeeSelectionCampaignId(UUID.randomUUID()),
                year, votingDeadline, null, groupIds);
        return new FeeSelectionCampaignWithGroups(publication, groups);
    }

    public record FeeSelectionCampaignWithGroups(FeeSelectionCampaign publication, List<MembershipFeeGroup> groups) {}

    public static FeeSelectionCampaign reconstruct(FeeSelectionCampaignId id, int year,
                                                   LocalDate votingDeadline,
                                                   @Nullable Instant deadlineProcessedAt,
                                                   List<MembershipFeeGroupId> publishedGroupIds) {
        return reconstruct(id, year, votingDeadline, deadlineProcessedAt, publishedGroupIds, null);
    }

    public static FeeSelectionCampaign reconstruct(FeeSelectionCampaignId id, int year,
                                                   LocalDate votingDeadline,
                                                   @Nullable Instant deadlineProcessedAt,
                                                   List<MembershipFeeGroupId> publishedGroupIds,
                                                   @Nullable AuditMetadata auditMetadata) {
        FeeSelectionCampaign publication = new FeeSelectionCampaign(id, year, votingDeadline, deadlineProcessedAt,
                publishedGroupIds);
        if (auditMetadata != null) {
            publication.updateAuditMetadata(auditMetadata);
        }
        return publication;
    }

    @Override
    public FeeSelectionCampaignId getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    public LocalDate getVotingDeadline() {
        return votingDeadline;
    }

    @Nullable
    public Instant getDeadlineProcessedAt() {
        return deadlineProcessedAt;
    }

    public List<MembershipFeeGroupId> getPublishedGroupIds() {
        return Collections.unmodifiableList(publishedGroupIds);
    }

    public boolean isProcessed() {
        return deadlineProcessedAt != null;
    }

    public boolean isClosed(LocalDate today) {
        return isProcessed() || today.isAfter(votingDeadline);
    }

    public void changeDeadline(LocalDate newDeadline, LocalDate today) {
        Assert.notNull(newDeadline, "NewDeadline is required");
        Assert.notNull(today, "Today is required");
        if (isClosed(today)) {
            throw new CampaignClosedException();
        }
        if (newDeadline.isBefore(today)) {
            throw new DeadlineNotInFutureException(newDeadline);
        }
        this.votingDeadline = newDeadline;
    }

    public void markProcessed(Instant at) {
        Assert.notNull(at, "ProcessedAt is required");
        this.deadlineProcessedAt = at;
    }
}
