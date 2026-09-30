package com.klabis.members.application;

import com.klabis.members.MemberId;
import com.klabis.members.domain.Member;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public interface MemberAccountActivationPort {

    /**
     * Whether an administrator may start the activation of the member's account: the member is a minor
     * with an own e-mail and the account still awaits activation.
     */
    boolean isAvailableFor(Member member);

    /**
     * Sends an activation link to the minor's own e-mail.
     *
     * @throws MemberNotFoundException                                     if no member with the given id exists
     * @throws com.klabis.common.exceptions.BusinessRuleViolationException if {@link #isAvailableFor} does not hold
     */
    void sendAccountActivation(MemberId memberId);
}
