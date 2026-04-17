package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.groups.traininggroup.TrainingGroupId;

record TrainingGroupSummaryResponse(TrainingGroupId id, String name, int minAge, int maxAge, int memberCount) {
}
