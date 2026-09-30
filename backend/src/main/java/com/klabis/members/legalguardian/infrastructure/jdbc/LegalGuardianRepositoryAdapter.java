package com.klabis.members.legalguardian.infrastructure.jdbc;

import com.klabis.common.users.UserId;
import com.klabis.members.legalguardian.domain.LegalGuardian;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@SecondaryAdapter
@Repository
class LegalGuardianRepositoryAdapter implements LegalGuardianRepository {

    private final LegalGuardianJdbcRepository jdbcRepository;

    LegalGuardianRepositoryAdapter(LegalGuardianJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public LegalGuardian save(LegalGuardian legalGuardian) {
        return jdbcRepository.save(LegalGuardianMemento.from(legalGuardian)).toLegalGuardian();
    }

    @Override
    public void delete(LegalGuardian legalGuardian) {
        jdbcRepository.deleteById(legalGuardian.getId().uuid());
    }

    @Override
    public Optional<LegalGuardian> findById(UserId id) {
        return jdbcRepository.findById(id.uuid()).map(LegalGuardianMemento::toLegalGuardian);
    }

    @Override
    public List<LegalGuardian> findAllByIds(Collection<UserId> ids) {
        List<UUID> uuids = ids.stream().map(UserId::uuid).toList();
        return StreamSupport.stream(jdbcRepository.findAllById(uuids).spliterator(), false)
                .map(LegalGuardianMemento::toLegalGuardian)
                .toList();
    }

    @Override
    public List<LegalGuardian> findAll() {
        return StreamSupport.stream(jdbcRepository.findAll().spliterator(), false)
                .map(LegalGuardianMemento::toLegalGuardian)
                .toList();
    }

    @Override
    public Optional<LegalGuardian> findByEmail(String email) {
        return jdbcRepository.findByEmailEqualsIgnoreCase(email.trim()).map(LegalGuardianMemento::toLegalGuardian);
    }
}
