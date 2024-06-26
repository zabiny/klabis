package club.klabis.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Configuration
public class RequestsLoggingConfiguration {

    @Component
    public static class LogVariablesFilter extends HttpFilter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            MDC.put("URL", "%s %s".formatted(((HttpServletRequest) request).getMethod(), ((HttpServletRequest) request).getRequestURI()));
            super.doFilter(request, response, chain);
            MDC.remove("URL");
        }
    }

    @Bean
    public FilterRegistrationBean<LogVariablesFilter> logVarsFilterReg(LogVariablesFilter filter) {
        FilterRegistrationBean<LogVariablesFilter> registrationBean = new FilterRegistrationBean<>(filter);
        //Assign a high priority so that this filter loads before other registered filters such as those loaded via Spring Security.
        registrationBean.setOrder(Integer.MIN_VALUE);
        return registrationBean;
    }

}
