package com.klabis.members.infrastructure.orissync;

import java.util.Map;

/**
 * Normalises an ORIS bare national phone number into Klabis's required E.164 form
 * (design.md D6).
 * <p>
 * A number already carrying a {@code +} is taken as-is — it is already unambiguous. A
 * bare number is prefixed with the dialling code inferred from the member's
 * country/nationality, but only for countries this normaliser explicitly knows;
 * anything else maps to {@code null} rather than a guess, since an unreachable wrong
 * number is worse than a missing one. Deliberately narrow: only {@code CZ} is handled
 * confidently for now, per design.md's own note that this mapping is the one most
 * likely to need tuning against real club data (foreign members, {@code 00}-prefixed
 * numbers, extensions).
 */
final class PhoneNumberNormalizer {

    private static final Map<String, String> DIALLING_PREFIXES = Map.of("CZ", "+420");

    private PhoneNumberNormalizer() {
    }

    static String normalize(String rawNumber, String countryCode) {
        if (rawNumber == null || rawNumber.isBlank()) {
            return null;
        }

        String trimmed = rawNumber.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }

        if (countryCode == null) {
            return null;
        }

        String prefix = DIALLING_PREFIXES.get(countryCode.trim().toUpperCase());
        return prefix != null ? prefix + trimmed : null;
    }
}
