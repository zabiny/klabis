package com.klabis.members.infrastructure.jdbc;

import com.klabis.common.pagination.TranslatedPageable;
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
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@SecondaryAdapter
@Repository
class MemberRepositoryAdapter implements MemberRepository {

    private static final Map<String, String> DOMAIN_TO_DB_COLUMN = Map.of(
            "firstName", "first_name",
            "lastName", "last_name",
            "registrationNumber", "registration_number"
    );

    private final MemberJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    public MemberRepositoryAdapter(MemberJdbcRepository jdbcRepository, JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Override
    public int countByBirthYear(int birthYear) {
        return jdbcRepository.countByBirthYear(birthYear);
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
    public List<Member> findAll() {
        return StreamSupport.stream(jdbcRepository.findAll().spliterator(), false)
                .map(MemberMemento::toMember)
                .toList();
    }

    @Override
    public List<Member> findAll(MemberFilter filter) {
        Query criteriaQuery = buildCriteriaQuery(filter);
        return jdbcAggregateTemplate.findAll(criteriaQuery, MemberMemento.class)
                .stream()
                .map(MemberMemento::toMember)
                .toList();
    }

    @Override
    public Page<Member> findAll(MemberFilter filter, Pageable pageable) {
        Pageable dbPageable = TranslatedPageable.translate(pageable, DOMAIN_TO_DB_COLUMN);
        Query criteriaQuery = buildCriteriaQuery(filter);

        List<Member> results = jdbcAggregateTemplate.findAll(criteriaQuery.with(dbPageable), MemberMemento.class)
                .stream()
                .map(MemberMemento::toMember)
                .toList();

        long total = jdbcAggregateTemplate.count(criteriaQuery, MemberMemento.class);

        return new PageImpl<>(results, pageable, total);
    }

    private Query buildCriteriaQuery(MemberFilter filter) {
        if (!filter.onlyActive()) {
            return Query.empty();
        }
        return Query.query(Criteria.where("active").isTrue());
    }
}
