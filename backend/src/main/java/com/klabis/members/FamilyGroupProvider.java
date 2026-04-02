package com.klabis.members;

import java.util.Optional;
import java.util.Set;

/**
 * Port for retrieving family group information for a member.
 * Implemented by the usergroups module to avoid a circular module dependency.
 */
public interface FamilyGroupProvider {

    Optional<FamilyGroupData> findFamilyGroupForMember(MemberId memberId);

    record FamilyGroupData(String groupName, Set<MemberId> ownerIds) {}
}
