package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.groups.freegroup.FreeGroupId;
import com.klabis.groups.freegroup.domain.InvitationId;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class FreeGroupOpenApiConfig {

    public FreeGroupOpenApiConfig() {
        SpringDocUtils.getConfig()
                .replaceWithClass(FreeGroupId.class, UUID.class)
                .replaceWithClass(InvitationId.class, UUID.class);
    }
}
