package com.klabis.members.traininggroup.infrastructure.restapi;

import com.klabis.members.traininggroup.domain.TrainingGroupId;

record TrainingGroupSummaryResponse(TrainingGroupId id, String name, int minAge, int maxAge, int memberCount) {
}
