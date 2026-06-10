package com.klabis.membershipfees.domain;

import com.klabis.membershipfees.MembershipFeeTierId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;
import java.util.Optional;

@SecondaryPort
public interface MembershipFeeTierRepository {

    MembershipFeeTier save(MembershipFeeTier level);

    Optional<MembershipFeeTier> findById(MembershipFeeTierId id);

    List<MembershipFeeTier> findAll();

    void delete(MembershipFeeTierId id);
}
