package com.klabis.members.familygroup.infrastructure.restapi;

import com.klabis.members.familygroup.domain.FamilyGroupId;

record FamilyGroupSummaryResponse(FamilyGroupId id, String name, int memberCount) {
}
