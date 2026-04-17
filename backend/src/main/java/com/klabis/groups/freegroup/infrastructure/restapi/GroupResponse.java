package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.groups.freegroup.FreeGroupId;
import org.springframework.hateoas.EntityModel;

import java.util.List;

record GroupResponse(FreeGroupId id, String name,
                    List<EntityModel<OwnerResponse>> owners,
                    List<EntityModel<FreeGroupMembershipResponse>> members,
                    List<EntityModel<PendingInvitationResponse>> pendingInvitations) {
}
