package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Collection;
import java.util.List;

@PrimaryPort
public interface GuardianContactResolver {

    /**
     * Users that cannot serve as legal guardian - unknown ones and members younger than 18 - are omitted
     * from the result; the caller decides whether that is an error.
     */
    List<GuardianContact> resolve(Collection<UserId> userIds);
}
