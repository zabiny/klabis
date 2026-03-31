package com.klabis.usergroups.infrastructure.restapi;

import java.util.UUID;

record TrainingGroupSummaryResponse(UUID id, String name, int minAge, int maxAge, int memberCount) {
}
