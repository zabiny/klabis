package club.klabis.shared.config.restapi.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class KlabisContextHandlerConfiguration implements WebMvcConfigurer {

    private final Logger LOG = LoggerFactory.getLogger(KlabisContextHandlerConfiguration.class);
    private final ContextHandler memberContextInterceptor;

    KlabisContextHandlerConfiguration(ContextHandler memberContextInterceptor) {
        this.memberContextInterceptor = memberContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LOG.info("Registering klabis request context handler");
        registry.addInterceptor(memberContextInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/static/**", "/assets/**", "/api-docs/**", "/favicon.ico", "/error");
    }

}
