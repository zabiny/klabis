package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.domain.MemberRegistrationBlockRepository;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.time.Instant;

@SecondaryAdapter
@Repository
class MemberRegistrationBlockRepositoryAdapter implements MemberRegistrationBlockRepository {

    private final MemberRegistrationBlockJdbcRepository jdbcRepository;

    MemberRegistrationBlockRepositoryAdapter(MemberRegistrationBlockJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public void block(MemberId memberId) {
        jdbcRepository.save(new MemberRegistrationBlockMemento(memberId.value(), Instant.now()));
    }

    @Override
    public void unblock(MemberId memberId) {
        jdbcRepository.deleteById(memberId.value());
    }

    @Override
    public boolean isBlocked(MemberId memberId) {
        return jdbcRepository.existsById(memberId.value());
    }
}
