package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Table(schema = "membershipfees", value = "membership_fee_group")
class MembershipFeeGroupMemento extends AbstractMembershipFeeMemento {

    @Column("source_level_id")
    private UUID sourceLevelId;

    @Column("name")
    private String name;

    @Column("group_year")
    private int year;

    @Column("yearly_fee_snapshot_amount")
    private BigDecimal yearlyFeeSnapshotAmount;

    @Column("yearly_fee_snapshot_currency")
    private String yearlyFeeSnapshotCurrency;

    @Column("status")
    private String status;

    @Column("voting_deadline")
    private LocalDate votingDeadline;

    @MappedCollection(idColumn = "membership_fee_group_id")
    private Set<MembershipPaymentRuleSnapshotMemento> rulesSnapshot = new HashSet<>();

    @MappedCollection(idColumn = "membership_fee_group_id")
    private Set<FeeGroupMembershipMemento> memberships = new HashSet<>();

    protected MembershipFeeGroupMemento() {
    }

    static MembershipFeeGroupMemento from(MembershipFeeGroup group) {
        MembershipFeeGroupMemento memento = new MembershipFeeGroupMemento();
        memento.id = group.getId().value();
        memento.sourceLevelId = group.getSourceLevelId().value();
        memento.name = group.getName();
        memento.year = group.getYear();
        memento.yearlyFeeSnapshotAmount = group.getYearlyFeeSnapshot().amount();
        memento.yearlyFeeSnapshotCurrency = group.getYearlyFeeSnapshot().currency().getCurrencyCode();
        memento.status = group.getStatus().name();
        memento.votingDeadline = group.getVotingDeadline();
        memento.rulesSnapshot = group.getRulesSnapshot().stream()
                .map(MembershipPaymentRuleSnapshotMemento::from)
                .collect(Collectors.toSet());
        memento.memberships = group.getMemberships().stream()
                .map(FeeGroupMembershipMemento::from)
                .collect(Collectors.toSet());
        memento.isNew = (group.getAuditMetadata() == null);
        memento.applyAudit(group.getAuditMetadata());
        return memento;
    }

    MembershipFeeGroup toGroup() {
        List<MembershipPaymentRule> snapshots = rulesSnapshot.stream()
                .map(MembershipPaymentRuleSnapshotMemento::toRule)
                .toList();
        Set<FeeGroupMembership> membershipSet = memberships.stream()
                .map(FeeGroupMembershipMemento::toMembership)
                .collect(Collectors.toSet());
        Money yearlyFee = Money.of(yearlyFeeSnapshotAmount, Currency.getInstance(yearlyFeeSnapshotCurrency));
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(id),
                new MembershipFeeTierId(sourceLevelId),
                name, year, votingDeadline, yearlyFee,
                PublishedLevelStatus.valueOf(status),
                snapshots, membershipSet, toAuditMetadata());
    }
}
