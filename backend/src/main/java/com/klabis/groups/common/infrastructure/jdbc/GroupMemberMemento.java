package com.klabis.groups.common.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("user_group_members")
class GroupMemberMemento {

    @Column("member_id")
    private UUID memberId;

    @Column("joined_at")
    private Instant joinedAt;

    protected GroupMemberMemento() {
    }

    GroupMemberMemento(UUID memberId, Instant joinedAt) {
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
