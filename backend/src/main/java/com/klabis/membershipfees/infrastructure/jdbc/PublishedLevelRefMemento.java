package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table(schema = "membershipfees", value = "fee_year_publication_level")
class PublishedLevelRefMemento {

    @Column("membership_fee_group_id")
    private UUID membershipFeeGroupId;

    protected PublishedLevelRefMemento() {
    }

    PublishedLevelRefMemento(UUID membershipFeeGroupId) {
        this.membershipFeeGroupId = membershipFeeGroupId;
    }

    UUID getMembershipFeeGroupId() {
        return membershipFeeGroupId;
    }
}
