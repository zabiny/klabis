package com.klabis.members.infrastructure;

import com.klabis.common.groups.domain.GroupMembership;
import com.klabis.common.groups.infrastructure.jdbc.GroupJdbcRepository;
import com.klabis.common.groups.infrastructure.jdbc.GroupMemento;
import com.klabis.common.users.UserId;
import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.members.MemberId;
import com.klabis.members.application.GroupParticipantsPort.GroupParticipants;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GroupParticipantsAdapter")
class GroupParticipantsAdapterTest {

    private static final UUID GROUP_ID = UUID.randomUUID();
    private static final MemberId OWNER = new MemberId(UUID.randomUUID());
    private static final MemberId MEMBER = new MemberId(UUID.randomUUID());

    private final GroupJdbcRepository groups = mock(GroupJdbcRepository.class);
    private final GroupParticipantsAdapter adapter = new GroupParticipantsAdapter(groups);

    @Test
    @DisplayName("free group: owners and members")
    void freeGroup() {
        when(groups.findById(GROUP_ID)).thenReturn(Optional.of(GroupMemento.from(
                FreeGroup.reconstruct(new FreeGroupId(GROUP_ID), "Free", Set.of(OWNER),
                        Set.of(GroupMembership.of(MEMBER)), Set.of(), Set.of(), null),
                GROUP_ID, FreeGroup.TYPE_DISCRIMINATOR, MemberId::value)));

        assertThat(adapter.findByGroupId(GROUP_ID)).contains(new GroupParticipants(Set.of(OWNER.uuid()), Set.of(MEMBER.uuid())));
    }

    @Test
    @DisplayName("training group: trainers and trainees")
    void trainingGroup() {
        when(groups.findById(GROUP_ID)).thenReturn(Optional.of(GroupMemento.from(
                TrainingGroup.reconstruct(new TrainingGroupId(GROUP_ID), "Training", Set.of(OWNER),
                        Set.of(GroupMembership.of(MEMBER)), new AgeRange(6, 12), null),
                GROUP_ID, TrainingGroup.TYPE_DISCRIMINATOR, MemberId::value)));

        assertThat(adapter.findByGroupId(GROUP_ID)).contains(new GroupParticipants(Set.of(OWNER.uuid()), Set.of(MEMBER.uuid())));
    }

    @Test
    @DisplayName("legal guardian group: guardians (member or not) and minors")
    void legalGuardianGroup() {
        UserId nonMember = new UserId(UUID.randomUUID());
        when(groups.findById(GROUP_ID)).thenReturn(Optional.of(GroupMemento.from(
                LegalGuardianGroup.reconstruct(new LegalGuardianGroupId(GROUP_ID), "Novákovi",
                        Set.of(nonMember, OWNER.toUserId()), Set.of(GroupMembership.of(MEMBER.toUserId())), null),
                GROUP_ID, LegalGuardianGroup.TYPE_DISCRIMINATOR, UserId::uuid)));

        assertThat(adapter.findByGroupId(GROUP_ID))
                .contains(new GroupParticipants(Set.of(nonMember.uuid(), OWNER.uuid()), Set.of(MEMBER.uuid())));
    }

    @Test
    @DisplayName("unknown group id is empty")
    void unknownGroup() {
        when(groups.findById(GROUP_ID)).thenReturn(Optional.empty());

        assertThat(adapter.findByGroupId(GROUP_ID)).isEmpty();
    }
}
