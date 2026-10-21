package com.klabis.members.infrastructure.jdbc;

import com.klabis.common.jdbc.FulltextSearch;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberFilter;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.RegistrationNumber;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@SecondaryAdapter
@Repository
class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;
    private final NamedParameterJdbcTemplate namedJdbc;

    public MemberRepositoryAdapter(MemberJdbcRepository jdbcRepository,
                                   JdbcAggregateTemplate jdbcAggregateTemplate,
                                   NamedParameterJdbcTemplate namedJdbc) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
        this.namedJdbc = namedJdbc;
    }

    @Override
    public int countByBirthYear(int birthYear) {
        return jdbcRepository.countByBirthYear(birthYear);
    }

    @Override
    public Optional<LocalDate> findDateOfBirth(MemberId id) {
        return jdbcRepository.findDateOfBirthById(id.uuid());
    }

    @Override
    public boolean existsAny() {
        return jdbcRepository.count() > 0;
    }

    @Override
    public Member save(Member member) {
        MemberMemento savedMemento = jdbcRepository.save(MemberMemento.from(member));
        return savedMemento.toMember();
    }

    @Override
    public Optional<Member> findById(MemberId memberId) {
        return jdbcRepository.findById(memberId.uuid())
                .map(MemberMemento::toMember);
    }

    @Override
    public List<Member> findAllByIds(Collection<MemberId> ids) {
        List<UUID> uuids = ids.stream().map(MemberId::uuid).toList();
        return StreamSupport.stream(jdbcRepository.findAllById(uuids).spliterator(), false)
                .map(MemberMemento::toMember)
                .toList();
    }

    @Override
    public Optional<Member> findByRegistrationNumber(RegistrationNumber registrationNumber) {
        return jdbcRepository.findByRegistrationNumber(registrationNumber.getValue())
                .map(MemberMemento::toMember);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return jdbcRepository.findByEmailEqualsIgnoreCase(email)
                .map(MemberMemento::toMember);
    }

    @Override
    public List<Member> findAllByEmail(String email) {
        return jdbcRepository.findAllByEmailEqualsIgnoreCase(email).stream()
                .map(MemberMemento::toMember)
                .toList();
    }

    @Override
    public List<Member> findAll() {
        return StreamSupport.stream(jdbcRepository.findAll().spliterator(), false)
                .map(MemberMemento::toMember)
                .toList();
    }

    @Override
    public List<Member> findAll(MemberFilter filter) {
        return findAll(filter, Pageable.unpaged()).getContent();
    }

    @Override
    public Page<Member> findAll(MemberFilter filter, Pageable pageable) {
        List<UUID> fulltextIds = filter.fulltextQuery() != null
                ? FulltextSearch.findIds(namedJdbc, "members.members", filter.fulltextQuery(),
                "first_name", "last_name", "registration_number")
                : null;
        if ((fulltextIds != null && fulltextIds.isEmpty()) || (filter.bornOn() != null && filter.bornOn().isEmpty())) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Query criteriaQuery = buildCriteriaQuery(filter, fulltextIds);
        List<Member> results = jdbcAggregateTemplate.findAll(criteriaQuery.with(pageable), MemberMemento.class)
                .stream()
                .map(MemberMemento::toMember)
                .toList();
        long total = pageable.isUnpaged() ? results.size() : jdbcAggregateTemplate.count(criteriaQuery, MemberMemento.class);
        return new PageImpl<>(results, pageable, total);
    }

    private Query buildCriteriaQuery(MemberFilter filter, List<UUID> fulltextIds) {
        List<Criteria> conditions = new ArrayList<>();

        if (fulltextIds != null) {
            conditions.add(Criteria.where("id").in(fulltextIds));
        }

        if (filter.status() == MemberFilter.StatusFilter.ACTIVE) {
            conditions.add(Criteria.where("active").isTrue());
        } else if (filter.status() == MemberFilter.StatusFilter.INACTIVE) {
            conditions.add(Criteria.where("active").isFalse());
        }

        if (filter.incompleteOnly()) {
            conditions.add(Criteria.where("dataIncomplete").isTrue());
        }

        if (filter.bornOn() != null) {
            conditions.add(Criteria.where("dateOfBirth").in(filter.bornOn()));
        }

        if (filter.bornOnOrBefore() != null) {
            conditions.add(Criteria.where("dateOfBirth").lessThanOrEquals(filter.bornOnOrBefore()));
        }

        if (conditions.isEmpty()) {
            return Query.empty();
        }

        Criteria combined = conditions.stream().reduce(Criteria::and).orElseThrow();
        return Query.query(combined);
    }
}
