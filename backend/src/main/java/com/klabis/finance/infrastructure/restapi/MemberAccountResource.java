package com.klabis.finance.infrastructure.restapi;

import com.klabis.finance.domain.MemberAccount;

import java.math.BigDecimal;
import java.util.UUID;

record MemberAccountResource(UUID memberId, BigDecimal balance, String currency) {

    static MemberAccountResource from(MemberAccount account) {
        return new MemberAccountResource(
                account.getId().uuid(),
                account.getBalance().amount(),
                account.getBalance().currency().getCurrencyCode()
        );
    }
}
