package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.domain.MemberRegistrationBlockRepository;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;

import java.time.Instant;

@SecondaryAdapter
@Repository
class MemberRegistrationBlockRepositoryAdapter implements MemberRegistrationBlockRepository {

    private final MemberRegistrationBlockJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    MemberRegistrationBlockRepositoryAdapter(MemberRegistrationBlockJdbcRepository jdbcRepository,
                                             JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Override
    public void block(MemberId memberId) {
        try {
            jdbcAggregateTemplate.insert(new MemberRegistrationBlockMemento(memberId.value(), Instant.now()));
        } catch (DataIntegrityViolationException e) {
            // already blocked — idempotent
        }
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
