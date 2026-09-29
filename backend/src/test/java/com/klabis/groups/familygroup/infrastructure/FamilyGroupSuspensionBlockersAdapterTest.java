package com.klabis.groups.familygroup.infrastructure;

import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.groups.familygroup.domain.FamilyGroupFilter;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("FamilyGroupSuspensionBlockersAdapter")
@ExtendWith(MockitoExtension.class)
class FamilyGroupSuspensionBlockersAdapterTest {

    private static final MemberId MEMBER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId OTHER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

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
        FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Rodina", MEMBER));
        when(repository.findOne(any(FamilyGroupFilter.class))).thenReturn(Optional.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER))
                .containsExactly(new OwnedGroup(group.getId().uuid().toString(), group.getName(), FamilyGroup.TYPE_DISCRIMINATOR));
    }

    @Test
    @DisplayName("does not report group when member is not its last parent")
    void ignoresGroupWhenMemberIsNotLast() {
        FamilyGroup group = FamilyGroup.create(new FamilyGroup.CreateFamilyGroup("Rodina", OTHER));
        when(repository.findOne(any(FamilyGroupFilter.class))).thenReturn(Optional.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("returns empty list when member has no groups")
    void returnsEmptyWhenNoGroups() {
        when(repository.findOne(any(FamilyGroupFilter.class))).thenReturn(Optional.empty());

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }
}
