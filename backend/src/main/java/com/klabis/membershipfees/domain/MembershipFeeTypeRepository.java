package com.klabis.membershipfees.domain;

import com.klabis.membershipfees.MembershipFeeTypeId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;
import java.util.Optional;

@SecondaryPort
public interface MembershipFeeTypeRepository {

    MembershipFeeType save(MembershipFeeType level);

    Optional<MembershipFeeType> findById(MembershipFeeTypeId id);

    List<MembershipFeeType> findAll();

    void delete(MembershipFeeTypeId id);
}
