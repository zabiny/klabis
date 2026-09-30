package com.klabis.members.legalguardiangroup.infrastructure.restapi;

import com.klabis.members.legalguardiangroup.LegalGuardianGroupId;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class LegalGuardianGroupOpenApiConfig {

    public LegalGuardianGroupOpenApiConfig() {
        SpringDocUtils.getConfig().replaceWithClass(LegalGuardianGroupId.class, UUID.class);
    }
}
