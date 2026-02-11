package com.klabis.members.persistence.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.members.*;
import com.klabis.users.UserId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Adapter that bridges between Members public API, MemberRepository domain interface and MemberJdbcRepository.
 * <p>
 * This adapter implements both:
 * <ul>
 *   <li>{@link Members} - public API for other modules (read-only operations)</li>
 *   <li>{@link com.klabis.members.persistence.MemberRepository MemberRepository} - internal API for members module</li>
 * </ul>
 * <p>
 * It handles conversion between Member entities and MemberMemento persistence objects.
 * <p>
 * Event publishing is handled automatically by Spring Modulith via the outbox pattern.
 * The MemberMemento delegates @DomainEvents and @AfterDomainEventPublication to the Member entity.
 */
@SecondaryAdapter
@Component
@Transactional
class MemberRepositoryAdapter implements Members, MemberRepository {

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
        MemberMemento memento = MemberMemento.from(member);
        MemberMemento saved = jdbcRepository.save(memento);

        // Update Member's audit metadata from saved memento (if available)
        AuditMetadata auditMetadata = saved.getAuditMetadata();
        if (auditMetadata != null) {
            member.updateAuditMetadata(auditMetadata);
        }

        // Return the same Member instance (now with updated audit metadata)
        return member;
    }

    @Override
    public Optional<Member> findById(UserId memberId) {
        return jdbcRepository.findById(memberId.uuid())
                .map(MemberMemento::toMember);
    }

    @Override
    public Optional<Member> findByRegistrationId(RegistrationNumber registrationId) {
        return jdbcRepository.findByRegistrationNumber(registrationId.getValue())
                .map(MemberMemento::toMember);
    }

    @Override
    public Optional<Member> findByRegistrationNumber(RegistrationNumber registrationNumber) {
        return findByRegistrationId(registrationNumber);
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
