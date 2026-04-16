package com.klabis.members.familygroup.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.groups.infrastructure.jdbc.GroupJdbcRepository;
import com.klabis.members.groups.infrastructure.jdbc.GroupMemento;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class FamilyGroupRepositoryAdapter implements FamilyGroupRepository {

    private final GroupJdbcRepository jdbcRepository;

    FamilyGroupRepositoryAdapter(GroupJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public FamilyGroup save(FamilyGroup group) {
        return jdbcRepository.save(GroupMemento.fromFamilyGroup(group)).toFamilyGroup();
    }

    @Override
    public Optional<FamilyGroup> findById(FamilyGroupId id) {
        return jdbcRepository.findByIdAndType(id.value(), FamilyGroup.TYPE_DISCRIMINATOR)
                .map(GroupMemento::toFamilyGroup);
    }

    @Override
    public List<FamilyGroup> findAll() {
        return jdbcRepository.findAllByType(FamilyGroup.TYPE_DISCRIMINATOR)
                .stream().map(GroupMemento::toFamilyGroup).toList();
    }

    @Override
    public Optional<FamilyGroup> findByMemberOrParent(MemberId memberId) {
        return jdbcRepository.findByMemberOrParentAndType(memberId.value(), FamilyGroup.TYPE_DISCRIMINATOR)
                .map(GroupMemento::toFamilyGroup);
    }

    @Override
    public void delete(FamilyGroupId id) {
        jdbcRepository.deleteByIdAndType(id.value(), FamilyGroup.TYPE_DISCRIMINATOR);
    }
}
