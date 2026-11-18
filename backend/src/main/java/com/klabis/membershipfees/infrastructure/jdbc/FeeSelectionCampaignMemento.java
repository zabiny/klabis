package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Table(schema = "membershipfees", value = "fee_selection_campaign")
class FeeSelectionCampaignMemento extends AbstractMembershipFeeMemento {

    @Column("publication_year")
    private int year;

    @Column("voting_deadline")
    private LocalDate votingDeadline;

    @Column("deadline_processed_at")
    private Instant deadlineProcessedAt;

    @MappedCollection(idColumn = "fee_selection_campaign_id")
    private Set<PublishedGroupRefMemento> publishedGroups = new HashSet<>();

    protected FeeSelectionCampaignMemento() {
    }

    static FeeSelectionCampaignMemento from(FeeSelectionCampaign publication) {
        FeeSelectionCampaignMemento memento = new FeeSelectionCampaignMemento();
        memento.id = publication.getId().value();
        memento.year = publication.getYear();
        memento.votingDeadline = publication.getVotingDeadline();
        memento.deadlineProcessedAt = publication.getDeadlineProcessedAt();
        memento.publishedGroups = publication.getPublishedGroupIds().stream()
                .map(groupId -> new PublishedGroupRefMemento(groupId.value()))
                .collect(Collectors.toSet());
        memento.isNew = (publication.getAuditMetadata() == null);
        memento.applyAudit(publication.getAuditMetadata());
        return memento;
    }

    FeeSelectionCampaign toPublication() {
        List<MembershipFeeGroupId> groupIds = publishedGroups.stream()
                .map(ref -> new MembershipFeeGroupId(ref.getMembershipFeeGroupId()))
                .toList();
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(id),
                year, votingDeadline, deadlineProcessedAt, groupIds, toAuditMetadata());
    }
}
