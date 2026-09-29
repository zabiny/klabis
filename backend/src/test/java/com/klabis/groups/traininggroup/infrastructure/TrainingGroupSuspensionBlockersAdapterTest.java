package com.klabis.groups.traininggroup.infrastructure;

import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
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

@DisplayName("TrainingGroupSuspensionBlockersAdapter")
@ExtendWith(MockitoExtension.class)
class TrainingGroupSuspensionBlockersAdapterTest {

    private static final MemberId MEMBER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId OTHER = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @Mock
    private TrainingGroupRepository repository;

    private TrainingGroupSuspensionBlockersAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TrainingGroupSuspensionBlockersAdapter(repository);
    }

    @Test
    @DisplayName("reports group when member is its last trainer")
    void reportsGroupWhenMemberIsLast() {
        TrainingGroup group = TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Žáci", MEMBER, new AgeRange(10, 12)));
        when(repository.findAll(any(TrainingGroupFilter.class))).thenReturn(List.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER))
                .containsExactly(new OwnedGroup(group.getId().uuid().toString(), group.getName(), TrainingGroup.TYPE_DISCRIMINATOR));
    }

    @Test
    @DisplayName("does not report group when member is not its last trainer")
    void ignoresGroupWhenMemberIsNotLast() {
        TrainingGroup group = TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Žáci", OTHER, new AgeRange(10, 12)));
        when(repository.findAll(any(TrainingGroupFilter.class))).thenReturn(List.of(group));

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }

    @Test
    @DisplayName("returns empty list when member has no groups")
    void returnsEmptyWhenNoGroups() {
        when(repository.findAll(any(TrainingGroupFilter.class))).thenReturn(List.of());

        assertThat(adapter.findGroupsBlockingSuspension(MEMBER)).isEmpty();
    }
}
