package com.klabis.common.mvc;


import com.klabis.common.logging.RequestLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@ComponentScan(
        basePackages = "com.klabis",
        includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = MvcComponent.class),
        useDefaultFilters = false
)
@Configuration
class MvcConfiguration implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(MvcConfiguration.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }

    public MvcConfiguration() {
        LOG.info("Init MvcConfiguration (include all @MvcComponent)");
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
    }
}
