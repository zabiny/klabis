package com.klabis.members.groups.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("user_group_owners")
class GroupOwnerMemento {

    @Column("member_id")
    private UUID memberId;

    protected GroupOwnerMemento() {
    }

    GroupOwnerMemento(UUID memberId) {
        this.memberId = memberId;
    }

    UUID getMemberId() {
        return memberId;
    }
}
