package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.ClubMember;

import java.util.Map;

/**
 * Narrow facade over the ORIS operations that need the club key (design.md D9).
 * Callers never see the key itself — they cannot leak it or be tempted to log it —
 * and a missing key fails with {@link ClubKeyNotSetException} before ORIS is ever
 * contacted, rather than surfacing as an ORIS-side rejection.
 * <p>
 * Shaped to gain further key-bearing operations later (entry submission is the
 * expected next one) without becoming a general-purpose client proxy — the other
 * ten {@code OrisApiClient} methods that need no key are called directly.
 */
interface OrisClubMembers {

    Map<String, ClubMember> listClubMembers();
}
