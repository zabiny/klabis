package com.klabis.members.legalguardiangroup.infrastructure;

import com.klabis.members.MemberId;
import com.klabis.members.MinorAgedOutEvent;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MinorAgedOutListener")
class MinorAgedOutListenerTest {

    @Mock
    private LegalGuardianGroupPort legalGuardianGroupPort;

    @InjectMocks
    private MinorAgedOutListener listener;

    @Test
    @DisplayName("should let the group service remove the aged out member from their group")
    void shouldRemoveMinorThroughGroupPort() {
        MemberId agedOut = new MemberId(UUID.randomUUID());

        listener.on(new MinorAgedOutEvent(agedOut));

        verify(legalGuardianGroupPort).removeMinor(agedOut);
    }
}
