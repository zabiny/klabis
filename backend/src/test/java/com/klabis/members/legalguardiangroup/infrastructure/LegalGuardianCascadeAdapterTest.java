package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.application.NotSoleGuardianGroupException;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegalGuardianCascadeAdapter")
class LegalGuardianCascadeAdapterTest {

    private static final MemberId GUARDIAN = new MemberId(UUID.randomUUID());
    private static final MemberId OTHER_GUARDIAN = new MemberId(UUID.randomUUID());
    private static final MemberId CHILD_A = new MemberId(UUID.randomUUID());
    private static final MemberId CHILD_B = new MemberId(UUID.randomUUID());

    @Mock
    private LegalGuardianGroupRepository repository;

    private LegalGuardianCascadeAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LegalGuardianCascadeAdapter(repository);
    }

    private static LegalGuardianGroup group(Set<MemberId> guardians, MemberId... minors) {
        Set<Guardian> guardianSet = guardians.stream()
                .map(g -> new Guardian(g.toUserId(), "Novák"))
                .collect(java.util.stream.Collectors.toSet());
        LegalGuardianGroup group = LegalGuardianGroup.create(guardianSet, minor(minors[0]));
        for (int i = 1; i < minors.length; i++) {
            group.addMinor(minor(minors[i]));
        }
        return group;
    }

    private static Minor minor(MemberId id) {
        return new Minor(id, LocalDate.now().minusYears(10));
    }

    @Test
    @DisplayName("returns the minors of a group the member is the sole guardian of")
    void returnsMinors() {
        LegalGuardianGroup group = group(Set.of(GUARDIAN), CHILD_A, CHILD_B);
        when(repository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThat(adapter.minorsOfSoleGuardianGroups(GUARDIAN, Set.of(group.getId())))
                .containsExactlyInAnyOrder(CHILD_A, CHILD_B);
    }

    @Test
    @DisplayName("rejects a group that has another guardian")
    void rejectsGroupWithAnotherGuardian() {
        LegalGuardianGroup group = group(Set.of(GUARDIAN, OTHER_GUARDIAN), CHILD_A);
        when(repository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> adapter.minorsOfSoleGuardianGroups(GUARDIAN, Set.of(group.getId())))
                .isInstanceOf(NotSoleGuardianGroupException.class);
    }

    @Test
    @DisplayName("rejects a group of someone else")
    void rejectsForeignGroup() {
        LegalGuardianGroup group = group(Set.of(OTHER_GUARDIAN), CHILD_A);
        when(repository.findById(group.getId())).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> adapter.minorsOfSoleGuardianGroups(GUARDIAN, Set.of(group.getId())))
                .isInstanceOf(NotSoleGuardianGroupException.class);
    }

    @Test
    @DisplayName("rejects an unknown group")
    void rejectsUnknownGroup() {
        LegalGuardianGroupId unknown = new LegalGuardianGroupId(UUID.randomUUID());
        when(repository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.minorsOfSoleGuardianGroups(GUARDIAN, Set.of(unknown)))
                .isInstanceOf(NotSoleGuardianGroupException.class);
    }
}
