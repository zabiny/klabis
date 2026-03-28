package com.klabis.oris.apiclient;

import com.klabis.oris.OrisIntegrationComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.HttpRequest;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;

@OrisIntegrationComponent
@Configuration
class OrisApiClientConfiguration implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OrisApiClientConfiguration.class);

    @Bean
    OrisApiClient orisApiClient(RestClient.Builder restClientBuilder, JsonMapper.Builder objectMapperBuilder) {

        SimpleDateFormat orisDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        orisDateTimeFormat.setTimeZone(TimeZone.getTimeZone("Europe/Prague"));

        JsonMapper objectMapper = objectMapperBuilder
                // ORIS API returns empty array in place of objects where such object is missing (instead of null)
                .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                // parse datetimes from ORIS in Prague timezone + honor timezone from data if present
                .defaultDateFormat(orisDateTimeFormat)
                .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .build();

        JacksonJsonHttpMessageConverter messageConverter = new JacksonJsonHttpMessageConverter(objectMapper);
        messageConverter.setSupportedMediaTypes(List.of(MediaType.valueOf("application/javascript")));

        RestClient restClient = restClientBuilder.baseUrl("https://oris.orientacnisporty.cz")
                .configureMessageConverters(c -> c.addCustomConverter(messageConverter))
                .requestInterceptor(this)
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter)
                .customArgumentResolver(new OrisEventListArgumentResolver())
                .build();

        return factory.createClient(OrisApiClient.class);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        LOG.debug("Request {} {}", request.getMethod(), request.getURI());

        var response = execution.execute(request, body);

        LOG.debug("Response status {}", response.getStatusCode());

        return response;
    }
}

class OrisEventListArgumentResolver implements HttpServiceArgumentResolver {
    private String formatLocalDateForOrisRequestParameter(Object value) {
        if (value instanceof LocalDate typedValue) {
            return typedValue.format(DateTimeFormatter.ISO_DATE);
        } else {
            return null;
        }
    }

    @Override
    public boolean resolve(Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
        if (parameter.getParameterType().equals(OrisEventListFilter.class)) {
            if (argument != null) {
                OrisEventListFilter typedArgument = (OrisEventListFilter) argument;

                if (typedArgument.dateFrom() != null) {
                    requestValues.addRequestParameter("datefrom",
                            formatLocalDateForOrisRequestParameter(typedArgument.dateFrom()));
                }

                if (typedArgument.dateTo() != null) {
                    requestValues.addRequestParameter("dateto",
                            formatLocalDateForOrisRequestParameter(typedArgument.dateTo()));
                }

                if (typedArgument.region() != null) {
                    requestValues.addRequestParameter("rg", typedArgument.region().toString());
                }

                requestValues.addRequestParameter("all", typedArgument.officialOnly() ? "0" : "1");
            }
            return true;
        }
        return false;
    }
}
