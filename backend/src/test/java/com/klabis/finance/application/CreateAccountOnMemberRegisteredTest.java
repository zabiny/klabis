package com.klabis.finance.application;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.members.MemberCreatedEvent;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateAccountOnMemberRegistered listener tests")
class CreateAccountOnMemberRegisteredTest {

    @Mock
    private MemberAccountRepository memberAccountRepository;

    @InjectMocks
    private CreateAccountOnMemberRegistered listener;

    @Test
    @DisplayName("creates a MemberAccount with zero balance when MemberCreatedEvent is received")
    void createsAccountWithZeroBalanceOnMemberRegistered() {
        MemberId memberId = new MemberId(UUID.randomUUID());
        MemberCreatedEvent event = MemberCreatedEventTestFactory.aMinimalEvent(memberId);

        listener.on(event);

        ArgumentCaptor<MemberAccount> captor = ArgumentCaptor.forClass(MemberAccount.class);
        verify(memberAccountRepository).save(captor.capture());
        MemberAccount savedAccount = captor.getValue();
        assertThat(savedAccount.getId()).isEqualTo(memberId);
        assertThat(savedAccount.getBalance().isZero()).isTrue();
    }
}
