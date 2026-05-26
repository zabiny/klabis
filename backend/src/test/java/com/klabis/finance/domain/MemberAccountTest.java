package com.klabis.finance.domain;

import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MemberAccount domain tests")
class MemberAccountTest {

    @Test
    @DisplayName("factory creates a new account with zero balance for a given MemberId")
    void createsNewAccountWithZeroBalance() {
        MemberId memberId = new MemberId(UUID.randomUUID());

        MemberAccount account = MemberAccount.openFor(memberId);

        assertThat(account.getId()).isEqualTo(memberId);
        assertThat(account.getBalance()).isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("two accounts for different members are not equal")
    void accountsForDifferentMembersAreNotEqual() {
        MemberAccount account1 = MemberAccount.openFor(new MemberId(UUID.randomUUID()));
        MemberAccount account2 = MemberAccount.openFor(new MemberId(UUID.randomUUID()));

        assertThat(account1).isNotEqualTo(account2);
    }
}
