package com.klabis.members.application;

import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;

/**
 * Secondary port owned by the members module. Reports groups that would be left without a
 * responsible person (last parent/owner/trainer) if the member was suspended.
 * Implemented by each group type (dependency direction: groups → members).
 */
@SecondaryPort
public interface MemberOwnedGroupsPort {

    List<OwnedGroup> findGroupsBlockingSuspension(MemberId memberId);
}
