package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipPaymentRuleSnapshot;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Table("membership_fee_group")
class MembershipFeeGroupMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

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

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("modified_at")
    private Instant lastModifiedAt;

    @LastModifiedBy
    @Column("modified_by")
    private String lastModifiedBy;

    @Version
    @Column("version")
    private Long version;

    @MappedCollection(idColumn = "membership_fee_group_id")
    private Set<MembershipPaymentRuleSnapshotMemento> rulesSnapshot = new HashSet<>();

    @MappedCollection(idColumn = "membership_fee_group_id")
    private Set<FeeGroupMembershipMemento> memberships = new HashSet<>();

    @Transient
    private boolean isNew = true;

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
        memento.rulesSnapshot = group.getRulesSnapshot().stream()
                .map(MembershipPaymentRuleSnapshotMemento::from)
                .collect(Collectors.toSet());
        memento.memberships = group.getMemberships().stream()
                .map(FeeGroupMembershipMemento::from)
                .collect(Collectors.toSet());
        memento.isNew = (group.getAuditMetadata() == null);
        applyAudit(memento, group.getAuditMetadata());
        return memento;
    }

    MembershipFeeGroup toGroup() {
        List<MembershipPaymentRuleSnapshot> snapshots = rulesSnapshot.stream()
                .map(MembershipPaymentRuleSnapshotMemento::toSnapshot)
                .toList();
        Set<FeeGroupMembership> membershipSet = memberships.stream()
                .map(FeeGroupMembershipMemento::toMembership)
                .collect(Collectors.toSet());
        Money yearlyFee = Money.of(yearlyFeeSnapshotAmount, Currency.getInstance(yearlyFeeSnapshotCurrency));
        AuditMetadata auditMetadata = createdAt != null
                ? new AuditMetadata(createdAt, createdBy, lastModifiedAt, lastModifiedBy, version)
                : null;
        return MembershipFeeGroup.reconstruct(
                new MembershipFeeGroupId(id),
                new MembershipFeeLevelId(sourceLevelId),
                name, year, yearlyFee,
                PublishedLevelStatus.valueOf(status),
                snapshots, membershipSet, auditMetadata);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    private static void applyAudit(MembershipFeeGroupMemento memento, AuditMetadata auditMetadata) {
        if (auditMetadata != null) {
            memento.createdAt = auditMetadata.createdAt();
            memento.createdBy = auditMetadata.createdBy();
            memento.lastModifiedAt = auditMetadata.lastModifiedAt();
            memento.lastModifiedBy = auditMetadata.lastModifiedBy();
            memento.version = auditMetadata.version();
        }
    }
}
