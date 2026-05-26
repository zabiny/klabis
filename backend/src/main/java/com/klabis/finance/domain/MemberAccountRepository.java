package com.klabis.finance.domain;

import com.klabis.members.MemberId;

import java.util.Optional;

public interface MemberAccountRepository {

    MemberAccount save(MemberAccount account);

    Optional<MemberAccount> findById(MemberId memberId);

    boolean existsById(MemberId memberId);
}
