package com.klabis.members.application;

import com.klabis.common.groups.domain.GroupNotFoundException;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberDtoBuilder;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GroupMemberOptionsService")
class GroupMemberOptionsServiceTest {

    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final MemberId OWNER = new MemberId(UUID.randomUUID());
    private static final MemberId MEMBER = new MemberId(UUID.randomUUID());
    private static final com.klabis.common.users.UserId NON_MEMBER_GUARDIAN = new com.klabis.common.users.UserId(UUID.randomUUID());

    private final GroupParticipantsPort groups = mock(GroupParticipantsPort.class);
    private final Members members = mock(Members.class);
    private final LegalGuardianRepository legalGuardians = mock(LegalGuardianRepository.class);
    private final GroupMemberOptionsService service = new GroupMemberOptionsService(groups, members, legalGuardians);

    @BeforeEach
    void knownPeople() {
        when(members.findByIds(any())).thenAnswer(invocation -> {
            Collection<MemberId> ids = invocation.getArgument(0);
            return Map.of(
                            OWNER, member(OWNER, "Jan", "Novák", "ZBM0101"),
                            MEMBER, member(MEMBER, "Eva", "Dvořáková", "ZBM0102"))
                    .entrySet().stream()
                    .filter(entry -> ids.contains(entry.getKey()))
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        });
        when(legalGuardians.findAllByIds(any())).thenReturn(List.of(LegalGuardian.create(
                new LegalGuardian.CreateLegalGuardian(NON_MEMBER_GUARDIAN, "Petr", "Svoboda", "petr@example.com",
                        "+420777111222"))));
    }

    @Test
    @DisplayName("owners and members, each named with the registration number")
    void ownersAndMembers() {
        stored(Set.of(OWNER.uuid()), Set.of(MEMBER.uuid()));

        assertThat(service.findOptions(GROUP_ID, null))
                .extracting(GroupParticipantOption::id, GroupParticipantOption::displayName)
                .containsExactlyInAnyOrder(
                        tuple(OWNER.uuid(), "Jan Novák (ZBM0101)"),
                        tuple(MEMBER.uuid(), "Eva Dvořáková (ZBM0102)"));
    }

    @Test
    @DisplayName("role filter returns only owners or only members")
    void roleFilter() {
        stored(Set.of(OWNER.uuid()), Set.of(MEMBER.uuid()));

        assertThat(service.findOptions(GROUP_ID, GroupParticipantRole.OWNER))
                .extracting(GroupParticipantOption::id).containsExactly(OWNER.uuid());
        assertThat(service.findOptions(GROUP_ID, GroupParticipantRole.MEMBER))
                .extracting(GroupParticipantOption::id).containsExactly(MEMBER.uuid());
    }

    @Test
    @DisplayName("a non-member legal guardian is named by name and e-mail")
    void nonMemberGuardian() {
        stored(Set.of(NON_MEMBER_GUARDIAN.uuid(), OWNER.uuid()), Set.of(MEMBER.uuid()));

        assertThat(service.findOptions(GROUP_ID, GroupParticipantRole.OWNER))
                .extracting(GroupParticipantOption::id, GroupParticipantOption::displayName)
                .containsExactlyInAnyOrder(
                        tuple(NON_MEMBER_GUARDIAN.uuid(), "Petr Svoboda (petr@example.com)"),
                        tuple(OWNER.uuid(), "Jan Novák (ZBM0101)"));
    }

    @Test
    @DisplayName("unknown group id is reported as not found")
    void unknownGroup() {
        when(groups.findByGroupId(GROUP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOptions(GROUP_ID, null)).isInstanceOf(GroupNotFoundException.class);
    }

    private void stored(Set<UUID> owners, Set<UUID> members) {
        when(groups.findByGroupId(GROUP_ID))
                .thenReturn(Optional.of(new GroupParticipantsPort.GroupParticipants(owners, members)));
    }

    private static MemberDto member(MemberId id, String first, String last, String registrationNumber) {
        return MemberDtoBuilder.builder().memberId(id.uuid()).firstName(first).lastName(last)
                .registrationNumber(registrationNumber).build();
    }
}
