package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.members.MemberId;
import com.klabis.members.MemberSuspendedEvent;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianSuspendedListener")
class GuardianSuspendedListenerTest {

    @Mock
    private LegalGuardianGroupPort legalGuardianGroupPort;

    @InjectMocks
    private GuardianSuspendedListener listener;

    @Test
    @DisplayName("should remove the suspended member from guardians of groups that keep other guardians")
    void shouldRemoveSuspendedMemberFromGuardians() {
        MemberId suspended = new MemberId(UUID.randomUUID());

        listener.on(new MemberSuspendedEvent(suspended, Instant.now()));

        verify(legalGuardianGroupPort).removeGuardianFromGroupsWithOtherGuardians(suspended.toUserId());
    }
}
