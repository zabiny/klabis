package com.klabis.members.infrastructure.restapi;

import com.klabis.common.ui.HalFormsOptionsDef;

import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

public final class LegalGuardianOptions {

    private LegalGuardianOptions() {
    }

    public static Map<String, HalFormsOptionsDef> forLegalGuardiansField() {
        return Map.of("legalGuardians", candidates(null));
    }

    public static Map<String, HalFormsOptionsDef> forRegistration() {
        return Map.of("legalGuardians", candidates(null),
                "legalGuardianUserId", candidates(LegalGuardianKind.LEGAL_GUARDIAN));
    }

    private static HalFormsOptionsDef candidates(LegalGuardianKind kind) {
        return HalFormsOptionsDef.remote(methodOn(LegalGuardianOptionsApi.class).listLegalGuardianOptions(kind));
    }
}
