package com.klabis.members.familygroup.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("family_group_children")
class FamilyGroupChildMemento {

    @Column("member_id")
    private UUID memberId;

    @Column("joined_at")
    private Instant joinedAt;

    protected FamilyGroupChildMemento() {
    }

    FamilyGroupChildMemento(UUID memberId, Instant joinedAt) {
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
