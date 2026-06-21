package com.klabis.finance.application;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.finance.domain.Money;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;
import com.klabis.finance.domain.TransactionType;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionQueryService")
class TransactionQueryServiceTest {

    private static final MemberId MEMBER_ID = new MemberId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    private static final UUID TX_UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final TransactionId TX_ID = new TransactionId(TX_UUID);

    @Mock
    private MemberAccountRepository memberAccountRepository;

    private TransactionQueryService service;

    @BeforeEach
    void setUp() {
        service = new TransactionQueryService(memberAccountRepository);
    }

    @Test
    @DisplayName("findTransaction returns the transaction when it exists in the member account")
    void findTransactionReturnsTransactionWhenFound() {
        Transaction tx = buildTransaction(TX_ID);
        MemberAccount account = MemberAccount.reconstruct(MEMBER_ID, Money.zero(), List.of(tx));
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));

        Transaction result = service.findTransaction(MEMBER_ID, TX_ID);

        assertThat(result).isSameAs(tx);
    }

    @Test
    @DisplayName("findTransaction throws TransactionNotFoundException when member account not found")
    void findTransactionThrowsWhenMemberAccountNotFound() {
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findTransaction(MEMBER_ID, TX_ID))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    @DisplayName("findTransaction throws TransactionNotFoundException when transaction not in account")
    void findTransactionThrowsWhenTransactionNotInAccount() {
        MemberAccount account = MemberAccount.reconstruct(MEMBER_ID, Money.zero(), List.of());
        when(memberAccountRepository.findById(MEMBER_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.findTransaction(MEMBER_ID, TX_ID))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    private Transaction buildTransaction(TransactionId id) {
        return Transaction.reconstruct(
                id,
                TransactionType.DEPOSIT,
                Money.ofCzk(BigDecimal.valueOf(200)),
                "Test deposit",
                Instant.now(),
                LocalDate.of(2026, 5, 1),
                new com.klabis.common.users.UserId(UUID.randomUUID()),
                null
        );
    }
}
