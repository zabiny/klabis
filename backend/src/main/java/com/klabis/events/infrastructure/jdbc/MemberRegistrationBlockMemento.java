package com.klabis.events.infrastructure.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table(schema = "events", value = "member_registration_block")
class MemberRegistrationBlockMemento {

    @Id
    @Column("member_id")
    private UUID memberId;

    @Column("blocked_at")
    private Instant blockedAt;

    MemberRegistrationBlockMemento() {
    }

    MemberRegistrationBlockMemento(UUID memberId, Instant blockedAt) {
        this.memberId = memberId;
        this.blockedAt = blockedAt;
    }

    UUID getMemberId() {
        return memberId;
    }
}
