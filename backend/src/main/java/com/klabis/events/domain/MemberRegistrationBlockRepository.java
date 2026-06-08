package com.klabis.events.domain;

import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

@Repository
@SecondaryPort
public interface MemberRegistrationBlockRepository {

    void block(MemberId memberId);

    void unblock(MemberId memberId);

    boolean isBlocked(MemberId memberId);
}
