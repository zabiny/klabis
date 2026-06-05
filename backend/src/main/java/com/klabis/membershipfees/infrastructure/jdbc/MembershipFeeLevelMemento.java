package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.MembershipFeeLevel;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
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

@Table("membership_fee_level")
class MembershipFeeLevelMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @Column("yearly_fee_amount")
    private BigDecimal yearlyFeeAmount;

    @Column("yearly_fee_currency")
    private String yearlyFeeCurrency;

    @MappedCollection(idColumn = "membership_fee_level_id")
    private Set<MembershipPaymentRuleMemento> rules = new HashSet<>();

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

    @Transient
    private boolean isNew = true;

    protected MembershipFeeLevelMemento() {
    }

    static MembershipFeeLevelMemento from(MembershipFeeLevel level) {
        MembershipFeeLevelMemento memento = new MembershipFeeLevelMemento();
        memento.id = level.getId().value();
        memento.name = level.getName();
        memento.yearlyFeeAmount = level.getYearlyFee().amount();
        memento.yearlyFeeCurrency = level.getYearlyFee().currency().getCurrencyCode();
        memento.rules = level.getRules().stream()
                .map(MembershipPaymentRuleMemento::from)
                .collect(Collectors.toSet());
        memento.isNew = (level.getAuditMetadata() == null);
        applyAudit(memento, level.getAuditMetadata());
        return memento;
    }

    MembershipFeeLevel toLevel() {
        List<MembershipPaymentRule> domainRules = rules.stream()
                .map(MembershipPaymentRuleMemento::toRule)
                .collect(Collectors.toList());

        Money yearlyFee = Money.of(yearlyFeeAmount, Currency.getInstance(yearlyFeeCurrency));

        AuditMetadata auditMetadata = createdAt != null
                ? new AuditMetadata(createdAt, createdBy, lastModifiedAt, lastModifiedBy, version)
                : null;

        return MembershipFeeLevel.reconstruct(new MembershipFeeLevelId(id), name, yearlyFee,
                domainRules, auditMetadata);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    private static void applyAudit(MembershipFeeLevelMemento memento, AuditMetadata auditMetadata) {
        if (auditMetadata != null) {
            memento.createdAt = auditMetadata.createdAt();
            memento.createdBy = auditMetadata.createdBy();
            memento.lastModifiedAt = auditMetadata.lastModifiedAt();
            memento.lastModifiedBy = auditMetadata.lastModifiedBy();
            memento.version = auditMetadata.version();
        }
    }
}
