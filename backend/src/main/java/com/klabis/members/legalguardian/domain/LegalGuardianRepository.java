package com.klabis.members.legalguardian.domain;

import com.klabis.common.users.UserId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LegalGuardianRepository {

    LegalGuardian save(LegalGuardian legalGuardian);

    Optional<LegalGuardian> findById(UserId id);

    List<LegalGuardian> findAllByIds(Collection<UserId> ids);

    List<LegalGuardian> findAll();

    Optional<LegalGuardian> findByEmail(String email);
}
