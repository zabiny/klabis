package com.klabis.members.infrastructure.orissync;

/**
 * Package-private read side of the club key (design.md D9): deliberately kept out of
 * {@link OrisClubKeyPort}, the public port, so nothing outside this package can even
 * reference the type needed to read the key back. Only {@link DefaultOrisClubMembers},
 * living in this same package, is meant to call {@link #currentKey()}.
 */
interface OrisClubKeyAccessor {

    boolean isSet();

    String currentKey();
}
