package com.klabis.groups.familygroup.infrastructure.restapi;

import com.klabis.groups.familygroup.FamilyGroupId;
import org.springframework.hateoas.EntityModel;

import java.util.List;

record FamilyGroupResponse(FamilyGroupId id, String name,
                           List<EntityModel<ParentResponse>> parents,
                           List<EntityModel<FamilyGroupMembershipResponse>> members) {
}
