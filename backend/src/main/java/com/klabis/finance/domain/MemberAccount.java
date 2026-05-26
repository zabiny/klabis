package com.klabis.finance.domain;

import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.members.MemberId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

@AggregateRoot
public class MemberAccount extends KlabisAggregateRoot<MemberAccount, MemberId> {

    @Identity
    private final MemberId id;

    private Money balance;

    private MemberAccount(MemberId id, Money balance) {
        this.id = id;
        this.balance = balance;
    }

    public static MemberAccount openFor(MemberId memberId) {
        return new MemberAccount(memberId, Money.zero());
    }

    public static MemberAccount reconstruct(MemberId id, Money balance) {
        return new MemberAccount(id, balance);
    }

    @Override
    public MemberId getId() {
        return id;
    }

    public Money getBalance() {
        return balance;
    }
}
