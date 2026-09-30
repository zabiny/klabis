package com.klabis.members.legalguardiangroup.domain;

import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;

import java.util.List;
import java.util.Optional;

public interface LegalGuardianGroupRepository {

    LegalGuardianGroup save(LegalGuardianGroup group);

    Optional<LegalGuardianGroup> findById(LegalGuardianGroupId id);

    List<LegalGuardianGroup> findAll(LegalGuardianGroupFilter filter);

    Optional<LegalGuardianGroup> findOne(LegalGuardianGroupFilter filter);

    void delete(LegalGuardianGroupId id);
}
