package com.klabis.members.application;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Secondary port: who owns and who belongs to a group of any type, as plain person ids.
 */
@SecondaryPort
public interface GroupParticipantsPort {

    Optional<GroupParticipants> findByGroupId(UUID groupId);

    record GroupParticipants(Set<UUID> ownerIds, Set<UUID> memberIds) {
    }
}
