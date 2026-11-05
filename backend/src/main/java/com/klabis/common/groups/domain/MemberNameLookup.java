package com.klabis.common.groups.domain;

import java.util.Optional;

/**
 * Resolves the display name of a group member from the identifier a group holds, which is a member id or
 * a user id depending on the group type. Only error responses use it, so a person who cannot be resolved
 * is simply left unnamed.
 */
public interface MemberNameLookup {

    Optional<String> nameOf(Object memberId);
}
