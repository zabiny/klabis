package com.klabis.finance.infrastructure.jdbc;

import com.klabis.finance.domain.MemberAccount;
import com.klabis.finance.domain.MemberAccountRepository;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@SecondaryAdapter
@Repository
@Component
class MemberAccountRepositoryAdapter implements MemberAccountRepository {

    private final MemberAccountJdbcRepository jdbcRepository;

    MemberAccountRepositoryAdapter(MemberAccountJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public MemberAccount save(MemberAccount account) {
        return jdbcRepository.save(MemberAccountMemento.from(account)).toMemberAccount();
    }

    @Override
    public Optional<MemberAccount> findById(MemberId memberId) {
        return jdbcRepository.findById(memberId.uuid()).map(MemberAccountMemento::toMemberAccount);
    }

    @Override
    public boolean existsById(MemberId memberId) {
        return jdbcRepository.existsById(memberId.uuid());
    }
}
