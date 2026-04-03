package com.klabis.members;

import java.util.List;

/**
 * Port for querying active member IDs within a specific age range.
 * Implemented by the members module, consumed by usergroups module to
 * auto-assign existing members when a new training group is created.
 */
public interface ActiveMembersByAgeProvider {

    /**
     * Returns IDs of all active members whose current age falls within [minAge, maxAge] inclusive.
     *
     * @param minAge minimum age (inclusive)
     * @param maxAge maximum age (inclusive)
     * @return list of matching active member IDs
     */
    List<MemberId> findActiveMemberIdsByAgeRange(int minAge, int maxAge);
}
