package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.membershipfees.FeeYearPublicationId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.domain.FeeYearPublication;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Table("fee_year_publication")
class FeeYearPublicationMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("publication_year")
    private int year;

    @Column("voting_deadline")
    private LocalDate votingDeadline;

    @Column("deadline_processed_at")
    private Instant deadlineProcessedAt;

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

    @MappedCollection(idColumn = "fee_year_publication_id")
    private Set<PublishedLevelRefMemento> publishedLevels = new HashSet<>();

    @Transient
    private boolean isNew = true;

    protected FeeYearPublicationMemento() {
    }

    static FeeYearPublicationMemento from(FeeYearPublication publication) {
        FeeYearPublicationMemento memento = new FeeYearPublicationMemento();
        memento.id = publication.getId().value();
        memento.year = publication.getYear();
        memento.votingDeadline = publication.getVotingDeadline();
        memento.deadlineProcessedAt = publication.getDeadlineProcessedAt();
        memento.publishedLevels = publication.getPublishedGroupIds().stream()
                .map(groupId -> new PublishedLevelRefMemento(groupId.value()))
                .collect(Collectors.toSet());
        memento.isNew = (publication.getAuditMetadata() == null);
        applyAudit(memento, publication.getAuditMetadata());
        return memento;
    }

    FeeYearPublication toPublication() {
        List<MembershipFeeGroupId> groupIds = publishedLevels.stream()
                .map(ref -> new MembershipFeeGroupId(ref.getMembershipFeeGroupId()))
                .toList();
        AuditMetadata auditMetadata = createdAt != null
                ? new AuditMetadata(createdAt, createdBy, lastModifiedAt, lastModifiedBy, version)
                : null;
        return FeeYearPublication.reconstruct(
                new FeeYearPublicationId(id),
                year, votingDeadline, deadlineProcessedAt, groupIds, auditMetadata);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    private static void applyAudit(FeeYearPublicationMemento memento, AuditMetadata auditMetadata) {
        if (auditMetadata != null) {
            memento.createdAt = auditMetadata.createdAt();
            memento.createdBy = auditMetadata.createdBy();
            memento.lastModifiedAt = auditMetadata.lastModifiedAt();
            memento.lastModifiedBy = auditMetadata.lastModifiedBy();
            memento.version = auditMetadata.version();
        }
    }
}
