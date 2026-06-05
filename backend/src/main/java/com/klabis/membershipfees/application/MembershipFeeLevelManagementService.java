package com.klabis.membershipfees.application;

import com.klabis.membershipfees.MembershipFeeLevelId;
import com.klabis.membershipfees.domain.MembershipFeeLevel;
import com.klabis.membershipfees.domain.MembershipFeeLevelRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class MembershipFeeLevelManagementService implements MembershipFeeLevelManagementPort {

    private final MembershipFeeLevelRepository repository;

    MembershipFeeLevelManagementService(MembershipFeeLevelRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public MembershipFeeLevelId createLevel(CreateLevelCommand command) {
        MembershipFeeLevel level = MembershipFeeLevel.create(command.name(), command.yearlyFee(), command.rules());
        return repository.save(level).getId();
    }

    @Transactional
    @Override
    public void editLevel(MembershipFeeLevelId id, EditLevelCommand command) {
        MembershipFeeLevel level = loadLevel(id);
        if (command.name() != null) {
            level.editName(command.name());
        }
        if (command.yearlyFee() != null) {
            level.editYearlyFee(command.yearlyFee());
        }
        if (command.rules() != null) {
            level.replaceRules(command.rules());
        }
        repository.save(level);
    }

    @Transactional(readOnly = true)
    @Override
    public MembershipFeeLevel getLevel(MembershipFeeLevelId id) {
        return loadLevel(id);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MembershipFeeLevel> listLevels() {
        return repository.findAll();
    }

    @Transactional
    @Override
    public void deleteLevel(MembershipFeeLevelId id) {
        loadLevel(id);
        repository.delete(id);
    }

    private MembershipFeeLevel loadLevel(MembershipFeeLevelId id) {
        return repository.findById(id)
                .orElseThrow(() -> new MembershipFeeLevelNotFoundException(id));
    }
}
