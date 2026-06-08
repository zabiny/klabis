package com.klabis.events.infrastructure.listeners;

import com.klabis.events.application.MemberRegistrationSanctionPort;
import com.klabis.membershipfees.MemberMissedFeeSelectionEvent;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipFeeEventListener")
class MembershipFeeEventListenerTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

    @Mock
    private MemberRegistrationSanctionPort sanctionPort;

    private MembershipFeeEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new MembershipFeeEventListener(sanctionPort);
    }

    @Test
    @DisplayName("should delegate to sanction port when member missed fee selection")
    void shouldDelegateToSanctionPort() {
        MemberMissedFeeSelectionEvent event = new MemberMissedFeeSelectionEvent(MEMBER_ID, 2026);

        listener.handle(event);

        verify(sanctionPort).applyMissedSelectionSanction(MEMBER_ID);
    }
}
