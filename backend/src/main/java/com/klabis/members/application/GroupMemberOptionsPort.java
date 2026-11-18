package com.klabis.members.application;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@PrimaryPort
public interface GroupMemberOptionsPort {

    /**
     * Owners and members of a group of any type, named for display; only the given role when {@code role} is set.
     *
     * @throws com.klabis.common.groups.domain.GroupNotFoundException when no group has the id
     */
    List<GroupParticipantOption> findOptions(UUID groupId, @Nullable GroupParticipantRole role);
}
