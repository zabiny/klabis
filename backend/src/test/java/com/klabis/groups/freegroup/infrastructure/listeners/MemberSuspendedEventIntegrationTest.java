package com.klabis.groups.freegroup.infrastructure.listeners;

import com.klabis.CleanupTestData;
import com.klabis.common.usergroup.InvitationStatus;
import com.klabis.groups.common.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.members.ActiveMembersByAgeProvider;
import com.klabis.members.MemberId;
import com.klabis.members.MemberSuspendedEvent;
import com.klabis.members.Members;
import com.klabis.members.domain.DeactivationReason;
import com.klabis.members.domain.RegistrationNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(value = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@CleanupTestData
@DisplayName("MemberSuspendedEvent integration — pending free-group invitation cancellation")
class MemberSuspendedEventIntegrationTest {

    private static final MemberId OWNER = new MemberId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private static final MemberId INVITEE = new MemberId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

    @MockitoBean
    @SuppressWarnings("unused")
    private Members members;

    @MockitoBean
    @SuppressWarnings("unused")
    private ActiveMembersByAgeProvider activeMembersByAgeProvider;

    @Autowired
    private FreeGroupRepository freeGroupRepository;

    @Test
    @DisplayName("should cancel pending invitation and publish FreeGroupInvitationCancelledEvent when MemberSuspendedEvent is received")
    void shouldCancelPendingInvitationAndPublishCancelledEvent(Scenario scenario) {
        FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Hiking Club", OWNER));
        group.invite(OWNER, INVITEE);
        FreeGroup saved = freeGroupRepository.save(group);

        MemberSuspendedEvent suspendedEvent = new MemberSuspendedEvent(
                INVITEE,
                new RegistrationNumber("ZBM9000"),
                DeactivationReason.OTHER,
                Instant.now(),
                OWNER.toUserId(),
                null
        );

        scenario.publish(suspendedEvent)
                .andWaitForStateChange(() -> freeGroupRepository
                        .findAll(FreeGroupFilter.all().withPendingInvitationFor(INVITEE))
                        .isEmpty())
                .andVerify(noMorePendingInvitations -> {
                    FreeGroup reloaded = freeGroupRepository.findById(saved.getId()).orElseThrow();

                    assertThat(reloaded.getPendingInvitations())
                            .as("pending invitations for the suspended member must be empty")
                            .isEmpty();

                    assertThat(reloaded.getInvitations())
                            .as("invitation record must still exist with CANCELLED status")
                            .hasSize(1);

                    var cancelled = reloaded.getInvitations().iterator().next();
                    assertThat(cancelled.getStatus()).isEqualTo(InvitationStatus.CANCELLED);
                    assertThat(cancelled.getInvitedUser()).isEqualTo(INVITEE.toUserId());
                    assertThat(cancelled.getCancellationReason())
                            .contains(MemberSuspendedListener.SYSTEM_CANCEL_REASON);
                    assertThat(cancelled.getCancelledBy()).isEmpty();
                });
    }

    @Test
    @DisplayName("should publish FreeGroupInvitationCancelledEvent with correct invitee and empty actor when MemberSuspendedEvent is received")
    void shouldPublishCancelledEventWithCorrectFields(Scenario scenario) {
        FreeGroup group = FreeGroup.create(new FreeGroup.CreateFreeGroup("Running Group", OWNER));
        group.invite(OWNER, INVITEE);
        FreeGroup saved = freeGroupRepository.save(group);

        MemberSuspendedEvent suspendedEvent = new MemberSuspendedEvent(
                INVITEE,
                new RegistrationNumber("ZBM9000"),
                DeactivationReason.OTHER,
                Instant.now(),
                OWNER.toUserId(),
                null
        );

        scenario.publish(suspendedEvent)
                .andWaitForStateChange(() -> {
                    FreeGroup reloaded = freeGroupRepository.findById(saved.getId()).orElseThrow();
                    var cancelledInvitations = reloaded.getInvitations().stream()
                            .filter(inv -> inv.getStatus() == InvitationStatus.CANCELLED)
                            .toList();
                    return cancelledInvitations.isEmpty() ? null : cancelledInvitations.get(0);
                })
                .andVerify(cancelledInvitation -> {
                    assertThat(cancelledInvitation.getInvitedUser()).isEqualTo(INVITEE.toUserId());
                    assertThat(cancelledInvitation.getCancellationReason())
                            .contains(MemberSuspendedListener.SYSTEM_CANCEL_REASON);
                    assertThat(cancelledInvitation.getCancelledBy()).isEmpty();
                });
    }
}
