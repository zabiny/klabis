package com.klabis.members.legalguardian.application;

import com.klabis.members.legalguardian.domain.LegalGuardian;

public record LegalGuardianProfile(LegalGuardian guardian, String loginName) {
}
