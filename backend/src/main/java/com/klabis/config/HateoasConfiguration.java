package com.klabis.config;

import com.klabis.common.mvc.MvcComponent;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;

@Configuration
@MvcComponent
@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL_FORMS, EnableHypermediaSupport.HypermediaType.HAL})
public class HateoasConfiguration {

}
