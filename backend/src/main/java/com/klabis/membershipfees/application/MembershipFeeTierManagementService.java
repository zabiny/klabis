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
        MembershipFeeTier tier = MembershipFeeTier.create(command.name(), command.yearlyFee());
        return repository.save(tier).getId();
    }

    @Transactional
    @Override
    public void editTier(MembershipFeeTierId id, EditTierCommand command) {
        MembershipFeeTier tier = loadTier(id);
        if (command.name() != null) {
            tier.editName(command.name());
        }
        if (command.yearlyFee() != null) {
            tier.editYearlyFee(command.yearlyFee());
        }
        repository.save(tier);
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
    public void addRule(MembershipFeeTierId tierId, AddRuleCommand command) {
        MembershipFeeTier tier = loadTier(tierId);
        tier.addRule(command.rule());
        repository.save(tier);
    }

    @Transactional
    @Override
    public void editRule(MembershipFeeTierId tierId, EditRuleCommand command) {
        MembershipFeeTier tier = loadTier(tierId);
        tier.editRuleValue(command.eventTypeId(), command.rankingShortName(), command.newValue());
        repository.save(tier);
    }

    @Transactional
    @Override
    public void removeRule(MembershipFeeTierId tierId, RemoveRuleCommand command) {
        MembershipFeeTier tier = loadTier(tierId);
        tier.removeRule(command.eventTypeId(), command.rankingShortName());
        repository.save(tier);
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
