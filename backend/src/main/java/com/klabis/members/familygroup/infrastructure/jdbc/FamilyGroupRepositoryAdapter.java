package com.klabis.members.familygroup.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.members.familygroup.domain.FamilyGroup;
import com.klabis.members.familygroup.domain.FamilyGroupId;
import com.klabis.members.familygroup.domain.FamilyGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class FamilyGroupRepositoryAdapter implements FamilyGroupRepository {

    private final FamilyGroupJdbcRepository jdbcRepository;

    FamilyGroupRepositoryAdapter(FamilyGroupJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public FamilyGroup save(FamilyGroup group) {
        return jdbcRepository.save(FamilyGroupMemento.from(group)).toFamilyGroup();
    }

    @Override
    public Optional<FamilyGroup> findById(FamilyGroupId id) {
        return jdbcRepository.findById(id.value()).map(FamilyGroupMemento::toFamilyGroup);
    }

    @Override
    public List<FamilyGroup> findAll() {
        List<FamilyGroup> result = new ArrayList<>();
        jdbcRepository.findAll().forEach(m -> result.add(m.toFamilyGroup()));
        return result;
    }

    @Override
    public Optional<FamilyGroup> findByMemberOrParent(MemberId memberId) {
        return jdbcRepository.findByMemberOrParent(memberId.value())
                .map(FamilyGroupMemento::toFamilyGroup);
    }

    @Override
    public void delete(FamilyGroupId id) {
        jdbcRepository.deleteById(id.value());
    }
}
