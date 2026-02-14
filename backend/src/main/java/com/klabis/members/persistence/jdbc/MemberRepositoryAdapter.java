package com.klabis.members.persistence.jdbc;

import com.klabis.members.Member;
import com.klabis.members.RegistrationNumber;
import com.klabis.members.persistence.MemberRepository;
import com.klabis.users.UserId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Adapter that bridges between MemberRepository domain interface and MemberJdbcRepository.
 * <p>
 * This adapter implements:
 * <ul>
 *   <li>{@link com.klabis.members.persistence.MemberRepository MemberRepository} - domain repository interface</li>
 * </ul>
 * <p>
 * MemberRepository extends {@link Members} public API, so this adapter indirectly implements both.
 * <p>
 * It handles conversion between Member entities and MemberMemento persistence objects.
 * <p>
 * Event publishing is handled automatically by Spring Modulith via the outbox pattern.
 * The MemberMemento delegates @DomainEvents and @AfterDomainEventPublication to the Member entity.
 */
@SecondaryAdapter
@Repository
class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJdbcRepository jdbcRepository;

    public MemberRepositoryAdapter(MemberJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public int countByBirthYear(int birthYear) {
        return jdbcRepository.countByBirthYear(birthYear);
    }

    @Override
    public Member save(Member member) {
        // Convert Member to MemberMemento for persistence
        MemberMemento savedMemento = jdbcRepository.save(MemberMemento.from(member));
        return savedMemento.toMember();
    }

    @Override
    public Optional<Member> findById(UserId memberId) {
        return jdbcRepository.findById(memberId.uuid())
                .map(MemberMemento::toMember);
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
    public Page<Member> findAll(Pageable pageable) {
        return jdbcRepository.findAll(pageable)
                .map(MemberMemento::toMember);
    }
}
