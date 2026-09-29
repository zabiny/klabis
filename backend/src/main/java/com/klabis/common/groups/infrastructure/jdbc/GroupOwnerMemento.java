package com.klabis.common.groups.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table(schema = "groups", value = "user_group_owners")
class GroupOwnerMemento {

    @Column("owner_id")
    private UUID ownerId;

    protected GroupOwnerMemento() {
    }

    GroupOwnerMemento(UUID ownerId) {
        this.ownerId = ownerId;
    }

    UUID getOwnerId() {
        return ownerId;
    }
}
