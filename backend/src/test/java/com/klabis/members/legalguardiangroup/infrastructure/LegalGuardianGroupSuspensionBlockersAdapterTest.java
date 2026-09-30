package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Guardian;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup.Minor;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LegalGuardianGroupSuspensionBlockersAdapter")
@ExtendWith(MockitoExtension.class)
class LegalGuardianGroupSuspensionBlockersAdapterTest {

    private static final MemberId MEMBER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId OTHER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final Minor CHILD = new Minor(new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")),
            LocalDate.now().minusYears(9));

    @Mock
    private LegalGuardianGroupRepository repository;

    private LegalGuardianGroupSuspensionBlockersAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LegalGuardianGroupSuspensionBlockersAdapter(repository);
    }

    private static Guardian guardian(MemberId member) {
        return new Guardian(member.toUserId(), "Novák");
    }

    @Test
    @DisplayName("reports the group when the member is the sole guardian of its minors")
    void reportsGroupWhenMemberIsSoleGuardian() {
        LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(guardian(MEMBER)), CHILD);
        when(repository.findAll(any(LegalGuardianGroupFilter.class))).thenReturn(List.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).containsExactly(
                new OwnedGroup(group.getId().uuid().toString(), group.getName(), LegalGuardianGroup.TYPE_DISCRIMINATOR));
    }

    @Test
    @DisplayName("reports every group where the member is the sole guardian")
    void reportsEveryGroup() {
        LegalGuardianGroup first = LegalGuardianGroup.create(Set.of(guardian(MEMBER)), CHILD);
        LegalGuardianGroup second = LegalGuardianGroup.create(Set.of(guardian(MEMBER)),
                new Minor(new MemberId(UUID.randomUUID()), LocalDate.now().minusYears(4)));
        when(repository.findAll(any(LegalGuardianGroupFilter.class))).thenReturn(List.of(first, second));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).hasSize(2);
    }

    @Test
    @DisplayName("does not report a group with another guardian")
    void ignoresGroupWithCoGuardian() {
        LegalGuardianGroup group = LegalGuardianGroup.create(Set.of(guardian(MEMBER), guardian(OTHER)), CHILD);
        when(repository.findAll(any(LegalGuardianGroupFilter.class))).thenReturn(List.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("returns an empty list when the member guards nobody")
    void returnsEmptyWhenNoGroups() {
        when(repository.findAll(any(LegalGuardianGroupFilter.class))).thenReturn(List.of());

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("looks groups up by the member's user id as guardian")
    void looksUpGroupsByGuardianUserId() {
        when(repository.findAll(any(LegalGuardianGroupFilter.class))).thenReturn(List.of());

        adapter.findGroupsBlockingSuspension(MEMBER);

        ArgumentCaptor<LegalGuardianGroupFilter> captor = ArgumentCaptor.forClass(LegalGuardianGroupFilter.class);
        verify(repository).findAll(captor.capture());
        UserId expected = MEMBER.toUserId();
        assertThat(captor.getValue().guardianIs()).isEqualTo(expected);
    }
}
