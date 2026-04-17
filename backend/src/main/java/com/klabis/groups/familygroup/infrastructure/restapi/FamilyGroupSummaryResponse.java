package com.klabis.groups.familygroup.infrastructure.restapi;

import com.klabis.groups.familygroup.FamilyGroupId;

record FamilyGroupSummaryResponse(FamilyGroupId id, String name, int memberCount) {
}
