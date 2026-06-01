package com.klabis.finance.application;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.members.MemberId;
import com.klabis.members.application.MemberFinancialStatePort;
import com.klabis.members.MonetaryAmount;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Implements the members module's MemberFinancialStatePort by reading from the MemberAccount aggregate.
 * Dependency direction: finance → members (correct — finance knows about members, not vice versa).
 */
@SecondaryAdapter
@Component
class MemberFinancialStateAdapter implements MemberFinancialStatePort {

    private final MemberAccountRepository memberAccountRepository;

    MemberFinancialStateAdapter(MemberAccountRepository memberAccountRepository) {
        this.memberAccountRepository = memberAccountRepository;
    }

    @Override
    public MemberFinancialSnapshot getFinancialSnapshot(MemberId memberId) {
        return memberAccountRepository.findById(memberId)
                .map(this::toSnapshot)
                .orElseGet(() -> noDebtSnapshot(memberId));
    }

    private MemberFinancialSnapshot toSnapshot(MemberAccount account) {
        var balance = account.getBalance();
        var monetaryAmount = new MonetaryAmount(balance.amount(), balance.currency().getCurrencyCode());
        return new MemberFinancialSnapshot(account.getId(), monetaryAmount, balance.isNegative());
    }

    private MemberFinancialSnapshot noDebtSnapshot(MemberId memberId) {
        return new MemberFinancialSnapshot(memberId, new MonetaryAmount(BigDecimal.ZERO, "CZK"), false);
    }
}
