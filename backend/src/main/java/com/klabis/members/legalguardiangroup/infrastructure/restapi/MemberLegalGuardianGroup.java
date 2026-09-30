package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.members.legalguardiangroup.domain.LegalGuardianGroup;
import org.jspecify.annotations.Nullable;

/**
 * Legal guardian group of the member whose detail is being served, looked up once by the controller and read
 * back by {@link MemberLegalGuardianGroupLinkProcessor}.
 */
public record MemberLegalGuardianGroup(@Nullable LegalGuardianGroup group) {
}
