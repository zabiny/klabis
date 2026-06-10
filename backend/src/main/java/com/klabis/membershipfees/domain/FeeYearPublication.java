package com.klabis.membershipfees.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.membershipfees.FeeYearPublicationId;
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
public class FeeYearPublication extends KlabisAggregateRoot<FeeYearPublication, FeeYearPublicationId> {

    @Identity
    private final FeeYearPublicationId id;
    private final int year;
    private final LocalDate votingDeadline;
    @Nullable
    private Instant deadlineProcessedAt;
    private final List<MembershipFeeGroupId> publishedGroupIds;

    private FeeYearPublication(FeeYearPublicationId id, int year, LocalDate votingDeadline,
                                @Nullable Instant deadlineProcessedAt,
                                List<MembershipFeeGroupId> publishedGroupIds) {
        Assert.notNull(id, "FeeYearPublicationId is required");
        Assert.notNull(votingDeadline, "VotingDeadline is required");
        this.id = id;
        this.year = year;
        this.votingDeadline = votingDeadline;
        this.deadlineProcessedAt = deadlineProcessedAt;
        this.publishedGroupIds = new ArrayList<>(publishedGroupIds);
    }

    public static FeeYearPublicationWithGroups publish(int year, LocalDate votingDeadline,
                                                        List<MembershipFeeLevel> levels) {
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

        FeeYearPublication publication = new FeeYearPublication(
                new FeeYearPublicationId(UUID.randomUUID()),
                year, votingDeadline, null, groupIds);
        return new FeeYearPublicationWithGroups(publication, groups);
    }

    public record FeeYearPublicationWithGroups(FeeYearPublication publication, List<MembershipFeeGroup> groups) {}

    public static FeeYearPublication reconstruct(FeeYearPublicationId id, int year,
                                                  LocalDate votingDeadline,
                                                  @Nullable Instant deadlineProcessedAt,
                                                  List<MembershipFeeGroupId> publishedGroupIds) {
        return reconstruct(id, year, votingDeadline, deadlineProcessedAt, publishedGroupIds, null);
    }

    public static FeeYearPublication reconstruct(FeeYearPublicationId id, int year,
                                                  LocalDate votingDeadline,
                                                  @Nullable Instant deadlineProcessedAt,
                                                  List<MembershipFeeGroupId> publishedGroupIds,
                                                  @Nullable AuditMetadata auditMetadata) {
        FeeYearPublication publication = new FeeYearPublication(id, year, votingDeadline, deadlineProcessedAt,
                publishedGroupIds);
        if (auditMetadata != null) {
            publication.updateAuditMetadata(auditMetadata);
        }
        return publication;
    }

    @Override
    public FeeYearPublicationId getId() {
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

    public boolean isClosed(LocalDate today) {
        return today.isAfter(votingDeadline);
    }

    public void markProcessed(Instant at) {
        Assert.notNull(at, "ProcessedAt is required");
        this.deadlineProcessedAt = at;
    }
}
