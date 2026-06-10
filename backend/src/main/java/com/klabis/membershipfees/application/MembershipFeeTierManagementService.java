package com.klabis.membershipfees.application;

import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.MembershipFeeTier;
import com.klabis.membershipfees.domain.MembershipFeeTierRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class MembershipFeeTierManagementService implements MembershipFeeTierManagementPort {

    private final MembershipFeeTierRepository repository;

    MembershipFeeTierManagementService(MembershipFeeTierRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public MembershipFeeTierId createTier(CreateTierCommand command) {
        MembershipFeeTier level = MembershipFeeTier.create(command.name(), command.yearlyFee());
        return repository.save(level).getId();
    }

    @Transactional
    @Override
    public void editTier(MembershipFeeTierId id, EditTierCommand command) {
        MembershipFeeTier level = loadTier(id);
        if (command.name() != null) {
            level.editName(command.name());
        }
        if (command.yearlyFee() != null) {
            level.editYearlyFee(command.yearlyFee());
        }
        repository.save(level);
    }

    @Transactional(readOnly = true)
    @Override
    public MembershipFeeTier getTier(MembershipFeeTierId id) {
        return loadTier(id);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MembershipFeeTier> listTiers() {
        return repository.findAll();
    }

    @Transactional
    @Override
    public void deleteTier(MembershipFeeTierId id) {
        loadTier(id);
        repository.delete(id);
    }

    private MembershipFeeTier loadTier(MembershipFeeTierId id) {
        return repository.findById(id)
                .orElseThrow(() -> new MembershipFeeTierNotFoundException(id));
    }
}
