package com.klabis.members.familygroup.infrastructure.restapi;

import com.klabis.members.familygroup.domain.FamilyGroupId;
import org.springframework.hateoas.EntityModel;

import java.util.List;

record FamilyGroupResponse(FamilyGroupId id, String name,
                           List<EntityModel<ParentResponse>> parents,
                           List<EntityModel<FamilyGroupMembershipResponse>> members) {
}
