package com.klabis.members.membersgroup.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("members_group_owners")
class MembersGroupOwnerMemento {

    @Column("member_id")
    private UUID memberId;

    protected MembersGroupOwnerMemento() {
    }

    MembersGroupOwnerMemento(UUID memberId) {
        this.memberId = memberId;
    }

    UUID getMemberId() {
        return memberId;
    }
}
