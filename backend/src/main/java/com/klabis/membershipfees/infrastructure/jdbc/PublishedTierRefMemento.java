package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table(schema = "membershipfees", value = "fee_selection_campaign_level")
class PublishedTierRefMemento {

    @Column("membership_fee_group_id")
    private UUID membershipFeeGroupId;

    protected PublishedTierRefMemento() {
    }

    PublishedTierRefMemento(UUID membershipFeeGroupId) {
        this.membershipFeeGroupId = membershipFeeGroupId;
    }

    UUID getMembershipFeeGroupId() {
        return membershipFeeGroupId;
    }
}
