package com.klabis.usergroups.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("user_group_owners")
class UserGroupOwnerMemento {

    @Column("member_id")
    private UUID memberId;

    protected UserGroupOwnerMemento() {
    }

    UserGroupOwnerMemento(UUID memberId) {
        this.memberId = memberId;
    }

    UUID getMemberId() {
        return memberId;
    }
}
