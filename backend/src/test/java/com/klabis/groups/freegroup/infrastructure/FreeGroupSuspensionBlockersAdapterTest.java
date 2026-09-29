package com.klabis.groups.freegroup.infrastructure;

import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("FreeGroupSuspensionBlockersAdapter")
@ExtendWith(MockitoExtension.class)
class FreeGroupSuspensionBlockersAdapterTest {

    private static final MemberId MEMBER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId OTHER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @Mock
    private FreeGroupRepository repository;

    private FreeGroupSuspensionBlockersAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FreeGroupSuspensionBlockersAdapter(repository);
    }

    @Test
    @DisplayName("reports group when member is its last owner")
    void reportsGroupWhenMemberIsLast() {
        FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Volná skupina", MEMBER));
        when(repository.findAll(any(FreeGroupFilter.class))).thenReturn(List.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER))
                .containsExactly(new OwnedGroup(group.getId().uuid().toString(), group.getName(), FreeGroup.TYPE_DISCRIMINATOR));
    }

    @Test
    @DisplayName("does not report group when member is not its last owner")
    void ignoresGroupWhenMemberIsNotLast() {
        FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Volná skupina", OTHER));
        when(repository.findAll(any(FreeGroupFilter.class))).thenReturn(List.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("returns empty list when member has no groups")
    void returnsEmptyWhenNoGroups() {
        when(repository.findAll(any(FreeGroupFilter.class))).thenReturn(List.of());

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }
}
