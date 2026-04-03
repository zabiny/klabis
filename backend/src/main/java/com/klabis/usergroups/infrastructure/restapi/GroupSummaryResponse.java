package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.usergroups.UserGroupId;

record GroupSummaryResponse(UserGroupId id, String name) {
}
