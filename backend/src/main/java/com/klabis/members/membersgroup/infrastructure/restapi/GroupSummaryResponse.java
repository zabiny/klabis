package com.klabis.members.membersgroup.infrastructure.restapi;

import com.klabis.members.membersgroup.domain.MembersGroupId;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "groupSummaryResponseList")
record GroupSummaryResponse(MembersGroupId id, String name) {
}
