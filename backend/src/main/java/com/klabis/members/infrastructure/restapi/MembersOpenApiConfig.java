package com.klabis.members.infrastructure.restapi;

import com.klabis.members.MemberId;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class MembersOpenApiConfig {

    public MembersOpenApiConfig() {
        SpringDocUtils.getConfig().replaceWithClass(MemberId.class, UUID.class);
    }
}
