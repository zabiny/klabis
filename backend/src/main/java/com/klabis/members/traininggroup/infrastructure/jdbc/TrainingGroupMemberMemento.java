package com.klabis.members.traininggroup.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("training_group_members")
class TrainingGroupMemberMemento {

    @Column("member_id")
    private UUID memberId;

    @Column("joined_at")
    private Instant joinedAt;

    protected TrainingGroupMemberMemento() {
    }

    TrainingGroupMemberMemento(UUID memberId, Instant joinedAt) {
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
