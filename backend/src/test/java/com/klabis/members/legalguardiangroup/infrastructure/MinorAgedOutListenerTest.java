package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.MinorAgedOutEvent;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinorAgedOutListener")
class MinorAgedOutListenerTest {

    @Mock
    private LegalGuardianGroupRepository groupRepository;

    @InjectMocks
    private MinorAgedOutListener listener;

    private static final Guardian GUARDIAN = new Guardian(new UserId(UUID.randomUUID()), "Novák");

    private static Minor minor(MemberId id) {
        return new Minor(id, LocalDate.now().minusYears(10));
    }

    @Test
    @DisplayName("should remove the child and keep the group when siblings remain")
    void shouldRemoveChildAndKeepGroupWithSiblings() {
        MemberId agedOut = new MemberId(UUID.randomUUID());
        MemberId sibling = new MemberId(UUID.randomUUID());
        LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(GUARDIAN), minor(agedOut));
        group.addMinor(minor(sibling));
        when(groupRepository.findOne(LegalGuardianGroupFilter.all().withMinorIs(agedOut.toUserId())))
                .thenReturn(Optional.of(group));

        listener.on(new MinorAgedOutEvent(agedOut));

        assertThat(group.getMinors()).extracting(m -> m.memberId()).containsExactly(sibling);
        verify(groupRepository).save(group);
        verify(groupRepository, never()).delete(any());
    }

    @Test
    @DisplayName("should delete the group when no child remains")
    void shouldDeleteGroupWithoutChildren() {
        MemberId agedOut = new MemberId(UUID.randomUUID());
        LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(GUARDIAN), minor(agedOut));
        when(groupRepository.findOne(LegalGuardianGroupFilter.all().withMinorIs(agedOut.toUserId())))
                .thenReturn(Optional.of(group));

        listener.on(new MinorAgedOutEvent(agedOut));

        verify(groupRepository).delete(group.getId());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("should be idempotent when the child is in no group")
    void shouldBeIdempotent() {
        MemberId agedOut = new MemberId(UUID.randomUUID());
        when(groupRepository.findOne(LegalGuardianGroupFilter.all().withMinorIs(agedOut.toUserId())))
                .thenReturn(Optional.empty());

        listener.on(new MinorAgedOutEvent(agedOut));

        verify(groupRepository, never()).save(any());
        verify(groupRepository, never()).delete(any());
    }
}
