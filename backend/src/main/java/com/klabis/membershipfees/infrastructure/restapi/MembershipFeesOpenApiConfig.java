package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.EventTypeReference;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class MembershipFeesOpenApiConfig {

    public MembershipFeesOpenApiConfig() {
        SpringDocUtils.getConfig().replaceWithClass(EventTypeReference.class, UUID.class);
    }
}
