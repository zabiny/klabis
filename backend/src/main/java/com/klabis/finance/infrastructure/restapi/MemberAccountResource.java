package com.klabis.finance.infrastructure.restapi;

import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;

import java.math.BigDecimal;
import java.util.UUID;

record MemberAccountResource(UUID memberId, BigDecimal balance, String currency) {

    static MemberAccountResource fromBalance(MemberId memberId, Money balance) {
        return new MemberAccountResource(
                memberId.uuid(),
                balance.amount(),
                balance.currency().getCurrencyCode()
        );
    }
}
