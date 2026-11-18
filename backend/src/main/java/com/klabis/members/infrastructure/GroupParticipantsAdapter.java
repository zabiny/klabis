package com.klabis.members.infrastructure;

import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.infrastructure.jdbc.GroupJdbcRepository;
import com.klabis.members.application.GroupParticipantsPort;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@SecondaryAdapter
@Component
class GroupParticipantsAdapter implements GroupParticipantsPort {

    private final GroupJdbcRepository groups;

    GroupParticipantsAdapter(GroupJdbcRepository groups) {
        this.groups = groups;
    }

    @Override
    public Optional<GroupParticipants> findByGroupId(UUID groupId) {
        return groups.findById(groupId).map(group -> new GroupParticipants(
                group.ownerIds(Function.<UUID>identity()),
                group.memberships(Function.<UUID>identity()).stream()
                        .map(GroupMembership::memberId)
                        .collect(Collectors.toSet())));
    }
}
