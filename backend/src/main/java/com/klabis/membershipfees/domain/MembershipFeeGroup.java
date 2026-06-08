package com.klabis.membershipfees.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@AggregateRoot
public class MembershipFeeGroup extends KlabisAggregateRoot<MembershipFeeGroup, MembershipFeeGroupId> {

    @Identity
    private final MembershipFeeGroupId id;
    private final MembershipFeeLevelId sourceLevelId;
    private final String name;
    private final int year;
    private Money yearlyFeeSnapshot;
    private PublishedLevelStatus status;
    private final List<MembershipPaymentRuleSnapshot> rulesSnapshot;
    private final Set<FeeGroupMembership> memberships;

    private MembershipFeeGroup(MembershipFeeGroupId id, MembershipFeeLevelId sourceLevelId,
                                String name, int year, Money yearlyFeeSnapshot,
                                PublishedLevelStatus status,
                                List<MembershipPaymentRuleSnapshot> rulesSnapshot,
                                Set<FeeGroupMembership> memberships) {
        Assert.notNull(id, "MembershipFeeGroupId is required");
        Assert.notNull(sourceLevelId, "SourceLevelId is required");
        Assert.hasText(name, "Name is required");
        Assert.notNull(yearlyFeeSnapshot, "YearlyFeeSnapshot is required");
        Assert.notNull(status, "Status is required");
        this.id = id;
        this.sourceLevelId = sourceLevelId;
        this.name = name;
        this.year = year;
        this.yearlyFeeSnapshot = yearlyFeeSnapshot;
        this.status = status;
        this.rulesSnapshot = new ArrayList<>(rulesSnapshot);
        this.memberships = new HashSet<>(memberships);
    }

    public static MembershipFeeGroup createSnapshot(MembershipFeeLevelId sourceLevelId,
                                                     String name, int year, Money yearlyFeeSnapshot,
                                                     List<MembershipPaymentRuleSnapshot> rulesSnapshot) {
        return new MembershipFeeGroup(
                new MembershipFeeGroupId(UUID.randomUUID()),
                sourceLevelId, name, year, yearlyFeeSnapshot,
                PublishedLevelStatus.EDITABLE,
                rulesSnapshot != null ? rulesSnapshot : List.of(),
                Set.of());
    }

    public static MembershipFeeGroup reconstruct(MembershipFeeGroupId id,
                                                  MembershipFeeLevelId sourceLevelId,
                                                  String name, int year,
                                                  Money yearlyFeeSnapshot,
                                                  PublishedLevelStatus status,
                                                  List<MembershipPaymentRuleSnapshot> rulesSnapshot,
                                                  Set<FeeGroupMembership> memberships,
                                                  AuditMetadata auditMetadata) {
        MembershipFeeGroup group = new MembershipFeeGroup(
                id, sourceLevelId, name, year, yearlyFeeSnapshot, status, rulesSnapshot, memberships);
        if (auditMetadata != null) {
            group.updateAuditMetadata(auditMetadata);
        }
        return group;
    }

    @Override
    public MembershipFeeGroupId getId() {
        return id;
    }

    public MembershipFeeLevelId getSourceLevelId() {
        return sourceLevelId;
    }

    public String getName() {
        return name;
    }

    public int getYear() {
        return year;
    }

    public Money getYearlyFeeSnapshot() {
        return yearlyFeeSnapshot;
    }

    public PublishedLevelStatus getStatus() {
        return status;
    }

    public List<MembershipPaymentRuleSnapshot> getRulesSnapshot() {
        return Collections.unmodifiableList(rulesSnapshot);
    }

    public Set<FeeGroupMembership> getMemberships() {
        return Collections.unmodifiableSet(memberships);
    }

    public int memberCount() {
        return memberships.size();
    }

    public void editSnapshot(Money yearlyFee, List<MembershipPaymentRuleSnapshot> rules) {
        if (status != PublishedLevelStatus.EDITABLE) {
            throw new IllegalStateException("Cannot edit snapshot of a FROZEN MembershipFeeGroup");
        }
        Assert.notNull(yearlyFee, "YearlyFee is required");
        this.yearlyFeeSnapshot = yearlyFee;
        this.rulesSnapshot.clear();
        if (rules != null) {
            this.rulesSnapshot.addAll(rules);
        }
    }

    public void freeze() {
        this.status = PublishedLevelStatus.FROZEN;
    }

    public void addMember(MemberId memberId, LocalDate today, AssignmentSource source) {
        addMember(memberId, today, source, null);
    }

    public void addMember(MemberId memberId, LocalDate today, AssignmentSource source, @Nullable MemberId assignedBy) {
        Assert.notNull(memberId, "MemberId is required");
        Assert.notNull(today, "Today is required");
        Assert.notNull(source, "AssignmentSource is required");

        if (status == PublishedLevelStatus.FROZEN && source == AssignmentSource.MEMBER_CHOICE) {
            throw new VotingClosedException();
        }

        boolean alreadyMember = memberships.stream()
                .anyMatch(m -> m.memberId().equals(memberId));
        if (alreadyMember) {
            return;
        }

        memberships.add(new FeeGroupMembership(memberId, today, source, assignedBy));
    }

    public void removeMember(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        memberships.removeIf(m -> m.memberId().equals(memberId));
    }

    public boolean hasMember(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        return memberships.stream().anyMatch(m -> m.memberId().equals(memberId));
    }
}
