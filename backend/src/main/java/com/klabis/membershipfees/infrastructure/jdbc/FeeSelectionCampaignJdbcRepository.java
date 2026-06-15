package com.klabis.membershipfees.infrastructure.jdbc;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

interface FeeSelectionCampaignJdbcRepository extends CrudRepository<FeeSelectionCampaignMemento, UUID> {
}
