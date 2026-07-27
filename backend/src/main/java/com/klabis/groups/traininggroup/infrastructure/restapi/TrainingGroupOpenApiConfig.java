package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.groups.traininggroup.TrainingGroupId;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class TrainingGroupOpenApiConfig {

    public TrainingGroupOpenApiConfig() {
        SpringDocUtils.getConfig().replaceWithClass(TrainingGroupId.class, UUID.class);
    }
}
