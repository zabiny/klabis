package com.klabis.groups.membersgroup.infrastructure.restapi;

import com.klabis.groups.membersgroup.MembersGroupId;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "groupSummaryResponseList")
record GroupSummaryResponse(MembersGroupId id, String name) {
}
