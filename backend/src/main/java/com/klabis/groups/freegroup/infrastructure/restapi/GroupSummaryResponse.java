package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.groups.freegroup.FreeGroupId;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "groupSummaryResponseList")
record GroupSummaryResponse(FreeGroupId id, String name) {
}
