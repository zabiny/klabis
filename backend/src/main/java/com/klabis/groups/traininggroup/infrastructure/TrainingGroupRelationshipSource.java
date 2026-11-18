package com.klabis.groups.traininggroup.infrastructure;

import com.klabis.common.authorization.DelegatedGrants;
import com.klabis.common.authorization.RelationshipSource;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Trainers hold ownership of the groups they train. Trainees gain nothing, and training groups delegate nothing
 * over their members.
 */
@Component
class TrainingGroupRelationshipSource implements RelationshipSource {

    private final TrainingGroupRepository groups;

    TrainingGroupRelationshipSource(TrainingGroupRepository groups) {
        this.groups = groups;
    }

    @Override
    public Map<Authority, Set<TargetRef>> grantsOf(UserId userId) {
        return DelegatedGrants.ownershipOf(
                groups.findAll(TrainingGroupFilter.all().withTrainerIs(MemberId.fromUserId(userId))),
                TrainingGroupId::uuid);
    }
}
