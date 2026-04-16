package com.klabis.members.familygroup.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("family_group_members")
class FamilyGroupMemberMemento {

    @Column("member_id")
    private UUID memberId;

    @Column("joined_at")
    private Instant joinedAt;

    protected FamilyGroupMemberMemento() {
    }

    FamilyGroupMemberMemento(UUID memberId, Instant joinedAt) {
        this.memberId = memberId;
        this.joinedAt = joinedAt;
    }

    UUID getMemberId() {
        return memberId;
    }

    Instant getJoinedAt() {
        return joinedAt;
    }
}
