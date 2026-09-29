package com.klabis.groups.familygroup.infrastructure;

import com.klabis.common.users.UserId;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.groups.familygroup.domain.FamilyGroupFilter;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FamilyGroupSuspensionBlockersAdapter")
@ExtendWith(MockitoExtension.class)
class FamilyGroupSuspensionBlockersAdapterTest {

    private static final MemberId MEMBER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId OTHER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private static final UserId PARENT_WITHOUT_MEMBER_PROFILE =
            new UserId(UUID.fromString("33333333-3333-3333-3333-333333333333"));

    @Mock
    private FamilyGroupRepository repository;

    private FamilyGroupSuspensionBlockersAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FamilyGroupSuspensionBlockersAdapter(repository);
    }

    @Test
    @DisplayName("reports group when member is its last parent")
    void reportsGroupWhenMemberIsLast() {
        FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Rodina", MEMBER.toUserId()));
        when(repository.findOne(any(FamilyGroupFilter.class))).thenReturn(Optional.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER))
                .containsExactly(new OwnedGroup(group.getId().uuid().toString(), group.getName(), FamilyGroup.TYPE_DISCRIMINATOR));
    }

    @Test
    @DisplayName("does not report group when member is not its last parent")
    void ignoresGroupWhenMemberIsNotLast() {
        FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Rodina", OTHER.toUserId()));
        when(repository.findOne(any(FamilyGroupFilter.class))).thenReturn(Optional.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("returns empty list when member has no groups")
    void returnsEmptyWhenNoGroups() {
        when(repository.findOne(any(FamilyGroupFilter.class))).thenReturn(Optional.empty());

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("does not report group when the member is only a child of it")
    void ignoresGroupWhenMemberIsOnlyAChild() {
        FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Rodina", OTHER.toUserId()));
        group.addChild(MEMBER);
        when(repository.findOne(any(FamilyGroupFilter.class))).thenReturn(Optional.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("does not report group whose last parent is a member parent with another parent present")
    void ignoresGroupWhenMemberParentHasCoParent() {
        FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Rodina", MEMBER.toUserId()));
        group.addParent(OTHER.toUserId());
        when(repository.findOne(any(FamilyGroupFilter.class))).thenReturn(Optional.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("does not report a group parented only by a user without a member profile")
    void ignoresGroupWhoseOnlyParentHasNoMemberProfile() {
        // Such a parent has no MemberId, so this adapter is never asked about them; suspending
        // the group's child is unaffected by who parents the group.
        FamilyGroup group = FamilyGroup.create(
                new FamilyGroup.CreateFamilyGroup("Rodina", PARENT_WITHOUT_MEMBER_PROFILE));
        group.addChild(MEMBER);
        when(repository.findOne(any(FamilyGroupFilter.class))).thenReturn(Optional.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("looks the group up by the member's user id")
    void looksUpGroupByMemberUserId() {
        FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Rodina", MEMBER.toUserId()));
        when(repository.findOne(any(FamilyGroupFilter.class))).thenReturn(Optional.of(group));

        adapter.findGroupsBlockingSuspension(MEMBER);

        ArgumentCaptor<FamilyGroupFilter> captor = ArgumentCaptor.forClass(FamilyGroupFilter.class);
        verify(repository).findOne(captor.capture());
        assertThat(captor.getValue().memberOrParentIs()).isEqualTo(MEMBER.toUserId());
    }
}
