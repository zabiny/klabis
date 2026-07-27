package com.klabis.groups.familygroup.infrastructure.restapi;

import com.klabis.groups.familygroup.FamilyGroupId;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class FamilyGroupOpenApiConfig {

    public FamilyGroupOpenApiConfig() {
        SpringDocUtils.getConfig().replaceWithClass(FamilyGroupId.class, UUID.class);
    }
}
