package com.klabis.members.legalguardiangroup.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberId;

public class OnlyMinorsAllowedException extends BusinessRuleViolationException {

    public OnlyMinorsAllowedException(MemberId memberId) {
        super("Member %s is not a minor; only minors can be in a legal guardian group".formatted(memberId.uuid()));
    }
}
