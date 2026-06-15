package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.FeeGroupMembership;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Table(schema = "membershipfees", value = "membership_fee_group_members")
class FeeGroupMembershipMemento {

    @Id
    @Column("id")
    private UUID id;

    @Column("member_id")
    private UUID memberId;

    @Column("joined_at")
    private LocalDate joinedAt;

    @Column("assignment_source")
    private String assignmentSource;

    @Column("assigned_by")
    private UUID assignedBy;

    protected FeeGroupMembershipMemento() {
    }

    static FeeGroupMembershipMemento from(FeeGroupMembership membership) {
        FeeGroupMembershipMemento memento = new FeeGroupMembershipMemento();
        memento.id = UUID.randomUUID();
        memento.memberId = membership.memberId().value();
        memento.joinedAt = membership.joinedAt();
        memento.assignmentSource = membership.source().name();
        memento.assignedBy = membership.assignedBy() != null ? membership.assignedBy().value() : null;
        return memento;
    }

    FeeGroupMembership toMembership() {
        MemberId assignedByMemberId = assignedBy != null ? new MemberId(assignedBy) : null;
        return new FeeGroupMembership(
                new MemberId(memberId),
                joinedAt,
                AssignmentSource.valueOf(assignmentSource),
                assignedByMemberId);
    }
}
