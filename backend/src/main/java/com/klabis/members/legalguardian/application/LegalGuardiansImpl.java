package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;
import com.klabis.members.LegalGuardianDto;
import com.klabis.members.LegalGuardians;
import com.klabis.members.legalguardian.domain.LegalGuardianRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
class LegalGuardiansImpl implements LegalGuardians {

    private final LegalGuardianRepository legalGuardianRepository;

    LegalGuardiansImpl(LegalGuardianRepository legalGuardianRepository) {
        this.legalGuardianRepository = legalGuardianRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<LegalGuardianDto> findById(UserId userId) {
        return legalGuardianRepository.findById(userId).map(guardian -> new LegalGuardianDto(
                guardian.getId().uuid(), guardian.getFirstName(), guardian.getLastName(),
                guardian.getEmail().value(),
                LocalDateTime.ofInstant(guardian.getLastModifiedAt(), ZoneId.of("Europe/Prague"))));
    }
}
