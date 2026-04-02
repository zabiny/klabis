package com.klabis.members;

import java.util.Optional;
import java.util.Set;

/**
 * Port for retrieving training group information for a member.
 * Implemented by the usergroups module to avoid a circular module dependency.
 */
public interface TrainingGroupProvider {

    Optional<TrainingGroupData> findTrainingGroupForMember(MemberId memberId);

    record TrainingGroupData(String groupName, Set<MemberId> ownerIds) {}
}
