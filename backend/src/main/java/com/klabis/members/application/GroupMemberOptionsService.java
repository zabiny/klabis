package com.klabis.members.application;

import com.klabis.common.groups.domain.GroupNotFoundException;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.jmolecules.ddd.annotation.Service;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
class GroupMemberOptionsService implements GroupMemberOptionsPort {

    private static final Collator CZECH = Collator.getInstance(Locale.forLanguageTag("cs-CZ"));

    private final GroupParticipantsPort groups;
    private final Members members;
    private final LegalGuardianRepository legalGuardians;

    GroupMemberOptionsService(GroupParticipantsPort groups, Members members, LegalGuardianRepository legalGuardians) {
        this.groups = groups;
        this.members = members;
        this.legalGuardians = legalGuardians;
    }

    @Transactional(readOnly = true)
    @Override
    public List<GroupParticipantOption> findOptions(UUID groupId, @Nullable GroupParticipantRole role) {
        GroupParticipantsPort.GroupParticipants group = groups.findByGroupId(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group", groupId));

        Set<UUID> ids = new HashSet<>();
        if (role != GroupParticipantRole.MEMBER) {
            ids.addAll(group.ownerIds());
        }
        if (role != GroupParticipantRole.OWNER) {
            ids.addAll(group.memberIds());
        }
        return named(ids);
    }

    private List<GroupParticipantOption> named(Set<UUID> ids) {
        Map<MemberId, MemberDto> knownMembers = members.findByIds(ids.stream().map(MemberId::new).toList());
        Set<UserId> nonMembers = ids.stream()
                .filter(id -> !knownMembers.containsKey(new MemberId(id)))
                .map(UserId::new)
                .collect(Collectors.toSet());

        Stream<GroupParticipantOption> memberOptions = knownMembers.values().stream()
                .map(member -> new GroupParticipantOption(member.memberId(),
                        "%s %s (%s)".formatted(member.firstName(), member.lastName(), member.registrationNumber())));
        Stream<GroupParticipantOption> guardianOptions = nonMembers.isEmpty() ? Stream.empty()
                : legalGuardians.findAllByIds(nonMembers).stream()
                .map(guardian -> new GroupParticipantOption(guardian.getId().uuid(),
                        "%s (%s)".formatted(guardian.getName().fullName(), guardian.getEmail().value())));

        return Stream.concat(memberOptions, guardianOptions)
                .sorted(Comparator.comparing(GroupParticipantOption::displayName, CZECH))
                .toList();
    }
}
