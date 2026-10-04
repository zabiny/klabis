package com.klabis.common.authorization;

import com.klabis.common.users.Authority;

import java.util.Map;
import java.util.Set;

/**
 * Builds snapshots with grants taken as given, for tests outside {@code common.authorization}. Production code
 * cannot do this: its snapshots go through the filtering of {@link AuthorizationSnapshot#fromSources}.
 */
public final class TestSnapshots {

    private TestSnapshots() {
    }

    public static AuthorizationSnapshot of(Set<Authority> overAll, Map<Authority, Set<TargetRef>> overTargets) {
        return AuthorizationSnapshot.of(overAll, overTargets);
    }
}
