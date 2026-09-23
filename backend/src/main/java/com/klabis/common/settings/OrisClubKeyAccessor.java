package com.klabis.common.settings;

/**
 * Read side of the club key (design.md D9): kept out of {@link OrisClubKeyPort}, the
 * public port, so a caller that only needs to set, discard or check the key never gets a
 * way to read it back. A component that genuinely needs to attach the key to an outgoing
 * request injects this explicitly.
 * <p>
 * Public rather than package-private because the ORIS facade that attaches the key lives
 * in the {@code members} module ({@code members.infrastructure.orissync}), while storage
 * lives here — the two are no longer in the same package.
 */
public interface OrisClubKeyAccessor {

    boolean isSet();

    String currentKey();
}
