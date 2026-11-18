package com.klabis.membershipfees.application;

import com.klabis.membershipfees.MembershipFeeTypeId;
import com.klabis.membershipfees.domain.MembershipFeeType;
import com.klabis.membershipfees.domain.MembershipFeeTypeRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class MembershipFeeTypeManagementService implements MembershipFeeTypeManagementPort {

    private final MembershipFeeTypeRepository repository;

    MembershipFeeTypeManagementService(MembershipFeeTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @Override
    public MembershipFeeTypeId createFeeType(CreateTypeCommand command) {
        MembershipFeeType type = MembershipFeeType.create(command.name(), command.yearlyFee());
        return repository.save(type).getId();
    }

    @Transactional
    @Override
    public void editFeeType(MembershipFeeTypeId id, EditTypeCommand command) {
        MembershipFeeType type = loadType(id);
        if (command.name() != null) {
            type.editName(command.name());
        }
        if (command.yearlyFee() != null) {
            type.editYearlyFee(command.yearlyFee());
        }
        repository.save(type);
    }

    @Transactional(readOnly = true)
    @Override
    public MembershipFeeType getFeeType(MembershipFeeTypeId id) {
        return loadType(id);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MembershipFeeType> listFeeTypes() {
        return repository.findAll();
    }

    @Transactional
    @Override
    public void addRule(MembershipFeeTypeId typeId, AddRuleCommand command) {
        MembershipFeeType type = loadType(typeId);
        type.addRule(command.rule());
        repository.save(type);
    }

    @Transactional
    @Override
    public void editRule(MembershipFeeTypeId typeId, EditRuleCommand command) {
        MembershipFeeType type = loadType(typeId);
        type.editRuleValue(command.eventTypeId(), command.rankingShortName(), command.newValue());
        repository.save(type);
    }

    @Transactional
    @Override
    public void removeRule(MembershipFeeTypeId typeId, RemoveRuleCommand command) {
        MembershipFeeType type = loadType(typeId);
        type.removeRule(command.eventTypeId(), command.rankingShortName());
        repository.save(type);
    }

    @Transactional
    @Override
    public void deleteFeeType(MembershipFeeTypeId id) {
        loadType(id);
        repository.delete(id);
    }

    private MembershipFeeType loadType(MembershipFeeTypeId id) {
        return repository.findById(id)
                .orElseThrow(() -> new MembershipFeeTypeNotFoundException(id));
    }
}
