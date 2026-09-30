package com.klabis.members.legalguardiangroup.infrastructure.jdbc;

import com.klabis.common.groups.infrastructure.jdbc.GroupJdbcRepository;
import com.klabis.common.groups.infrastructure.jdbc.GroupMemento;
import com.klabis.common.users.UserId;
import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class LegalGuardianGroupRepositoryAdapter implements LegalGuardianGroupRepository {

    private final GroupJdbcRepository jdbcRepository;
    private final JdbcAggregateTemplate jdbcAggregateTemplate;

    LegalGuardianGroupRepositoryAdapter(GroupJdbcRepository jdbcRepository,
                                        JdbcAggregateTemplate jdbcAggregateTemplate) {
        this.jdbcRepository = jdbcRepository;
        this.jdbcAggregateTemplate = jdbcAggregateTemplate;
    }

    @Override
    public LegalGuardianGroup save(LegalGuardianGroup group) {
        return toDomain(jdbcRepository.save(fromDomain(group)));
    }

    @Override
    public Optional<LegalGuardianGroup> findById(LegalGuardianGroupId id) {
        return jdbcRepository.findByIdAndType(id.value(), LegalGuardianGroup.TYPE_DISCRIMINATOR)
                .map(this::toDomain);
    }

    @Override
    public List<LegalGuardianGroup> findAll(LegalGuardianGroupFilter filter) {
        return candidates(filter).stream()
                .map(this::toDomain)
                .filter(group -> matches(group, filter))
                .toList();
    }

    @Override
    public Optional<LegalGuardianGroup> findOne(LegalGuardianGroupFilter filter) {
        List<LegalGuardianGroup> results = findAll(filter);
        if (results.size() > 1) {
            throw new IllegalStateException(
                    "findOne expected at most 1 result but filter matched " + results.size() + " rows");
        }
        return results.stream().findFirst();
    }

    @Override
    public void delete(LegalGuardianGroupId id) {
        jdbcRepository.deleteByIdAndType(id.value(), LegalGuardianGroup.TYPE_DISCRIMINATOR);
    }

    // Narrows by SQL on one participant; the remaining conditions of the filter are then checked in memory,
    // since a user is in at most a handful of groups.
    private List<GroupMemento> candidates(LegalGuardianGroupFilter filter) {
        if (filter.guardianIs() != null) {
            return jdbcRepository.findByTrainerIdAndType(filter.guardianIs().uuid(),
                    LegalGuardianGroup.TYPE_DISCRIMINATOR);
        }
        if (filter.minorIs() != null) {
            return jdbcRepository.findByMemberIdAndType(filter.minorIs().uuid(),
                    LegalGuardianGroup.TYPE_DISCRIMINATOR);
        }
        if (filter.guardiansAre() != null && !filter.guardiansAre().isEmpty()) {
            UserId anyGuardian = filter.guardiansAre().iterator().next();
            return jdbcRepository.findByTrainerIdAndType(anyGuardian.uuid(), LegalGuardianGroup.TYPE_DISCRIMINATOR);
        }
        return jdbcAggregateTemplate.findAll(
                Query.query(Criteria.where("type").is(LegalGuardianGroup.TYPE_DISCRIMINATOR)), GroupMemento.class);
    }

    private static boolean matches(LegalGuardianGroup group, LegalGuardianGroupFilter filter) {
        return (filter.guardianIs() == null || group.isOwner(filter.guardianIs()))
               && (filter.minorIs() == null || group.hasMember(filter.minorIs()))
               && (filter.guardiansAre() == null || group.getGuardians().equals(filter.guardiansAre()));
    }

    private GroupMemento fromDomain(LegalGuardianGroup group) {
        return GroupMemento.from(group, group.getId().value(), LegalGuardianGroup.TYPE_DISCRIMINATOR, UserId::uuid);
    }

    private LegalGuardianGroup toDomain(GroupMemento memento) {
        return LegalGuardianGroup.reconstruct(new LegalGuardianGroupId(memento.getId()), memento.getName(),
                memento.ownerIds(UserId::new), memento.memberships(UserId::new), memento.auditMetadata());
    }
}
