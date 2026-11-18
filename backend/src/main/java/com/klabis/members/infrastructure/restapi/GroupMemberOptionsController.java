package com.klabis.members.infrastructure.restapi;

import com.klabis.members.application.GroupMemberOptionsPort;
import com.klabis.members.application.GroupParticipantOption;
import com.klabis.members.application.GroupParticipantRole;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
class GroupMemberOptionsController implements GroupMemberOptionsApi {

    private final GroupMemberOptionsPort groupMemberOptions;

    GroupMemberOptionsController(GroupMemberOptionsPort groupMemberOptions) {
        this.groupMemberOptions = groupMemberOptions;
    }

    @Override
    public ResponseEntity<List<MemberOptionResponse>> listGroupMemberOptions(UUID id, GroupRole role) {
        GroupParticipantRole participantRole = role != null ? GroupParticipantRole.valueOf(role.name()) : null;
        return ResponseEntity.ok(groupMemberOptions.findOptions(id, participantRole).stream()
                .map(GroupMemberOptionsController::toResponse)
                .toList());
    }

    private static MemberOptionResponse toResponse(GroupParticipantOption option) {
        return MemberOptionResponseBuilder.builder()
                .value(option.id().toString())
                .prompt(option.displayName())
                .build();
    }
}
