package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.usergroups.UserGroupId;

record TrainingGroupSummaryResponse(UserGroupId id, String name, int minAge, int maxAge, int memberCount) {
}
