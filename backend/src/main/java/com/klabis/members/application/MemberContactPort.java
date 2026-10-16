package com.klabis.members.application;

import com.klabis.common.users.UserId;
import com.klabis.members.domain.Member;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Set;

@PrimaryPort
public interface MemberContactPort {

    /**
     * Guardians are consulted only for a minor lacking own e-mail or phone.
     */
    EffectiveContact effectiveContactOf(Member member);

    /**
     * Like {@link #effectiveContactOf(Member)} for a caller that has already looked up the member's current guardians.
     */
    EffectiveContact effectiveContactOf(Member member, Set<UserId> guardians);
}
