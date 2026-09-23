package com.klabis.members.infrastructure.orissync;

import com.klabis.common.ui.HalResponseContext;

/**
 * Carries whether the ORIS club key is currently held, from {@code MemberController#listMembers}
 * to {@link MemberOrisImportAffordancePostprocessor} via {@link HalResponseContext}, rather than
 * injecting {@link com.klabis.common.settings.OrisClubKeyPort} into the postprocessor (design.md D11;
 * see the postprocessor's javadoc for why).
 */
public record ClubKeyHeld(boolean held) {
}
