package com.klabis.members.familygroup.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("family_group_parents")
class FamilyGroupParentMemento {

    @Column("member_id")
    private UUID memberId;

    protected FamilyGroupParentMemento() {
    }

    FamilyGroupParentMemento(UUID memberId) {
        this.memberId = memberId;
    }

    UUID getMemberId() {
        return memberId;
    }
}
