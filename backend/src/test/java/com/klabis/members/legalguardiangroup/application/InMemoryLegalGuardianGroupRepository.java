package com.klabis.members.legalguardiangroup.application;

import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupFilter;
import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroupRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryLegalGuardianGroupRepository implements LegalGuardianGroupRepository {

    private final Map<LegalGuardianGroupId, LegalGuardianGroup> groups = new LinkedHashMap<>();

    @Override
    public LegalGuardianGroup save(LegalGuardianGroup group) {
        groups.put(group.getId(), group);
        return group;
    }

    @Override
    public Optional<LegalGuardianGroup> findById(LegalGuardianGroupId id) {
        return Optional.ofNullable(groups.get(id));
    }

    @Override
    public List<LegalGuardianGroup> findAll(LegalGuardianGroupFilter filter) {
        return groups.values().stream()
                .filter(g -> filter.guardianIs() == null || g.isOwner(filter.guardianIs()))
                .filter(g -> filter.minorIs() == null || g.hasMember(filter.minorIs()))
                .filter(g -> filter.guardiansAre() == null || g.getGuardians().equals(filter.guardiansAre()))
                .toList();
    }

    @Override
    public Optional<LegalGuardianGroup> findOne(LegalGuardianGroupFilter filter) {
        return findAll(filter).stream().findFirst();
    }

    @Override
    public void delete(LegalGuardianGroupId id) {
        groups.remove(id);
    }
}
