package com.klabis.common.mvc;


import com.klabis.common.logging.RequestLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@ComponentScan(
        basePackages = "com.klabis",
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = MvcComponent.class),
        useDefaultFilters = false
)
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
class MvcConfiguration implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(MvcConfiguration.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }

    private final ObjectProvider<ObjectMapper> objectMapper;

    public MvcConfiguration(ObjectProvider<ObjectMapper> objectMapper) {
        this.objectMapper = objectMapper;
        LOG.info("Init MvcConfiguration (include all @MvcComponent)");
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
    }

    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add(new UnexpectedErrorResolver(objectMapper.getObject()));
    }

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> pwaMimeMappingsCustomizer() {
        return factory -> {
            MimeMappings mappings = new MimeMappings(MimeMappings.DEFAULT);
            mappings.add("webmanifest", "application/manifest+json");
            factory.setMimeMappings(mappings);
        };
    }
}
