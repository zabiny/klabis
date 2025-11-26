package club.klabis.oris.infrastructure.apiclient;

import club.klabis.oris.application.dto.OrisEventListFilter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Configuration
class OrisApiClientConfiguration implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OrisApiClientConfiguration.class);


    @Bean
    OrisApiClient orisApiClient(RestClient.Builder restClientBuilder, JsonMapper.Builder objectMapperBuilder) {

        SimpleDateFormat orisDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        orisDateTimeFormat.setTimeZone(TimeZone.getTimeZone("Europe/Prague"));

        ObjectMapper objectMapper = objectMapperBuilder
                // ORIS API returns empty array in place of objects where such object is missing (instead of null)
                .enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                // parse datetimes from ORIS in Prague timezone + honor timezone from data if is it present (otherwise adjust for Prague)
                .defaultDateFormat(orisDateTimeFormat)
                .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
                .build();

        JacksonJsonHttpMessageConverter messageConverter = new JacksonJsonHttpMessageConverter(objectMapperBuilder);
        messageConverter.setSupportedMediaTypes(List.of(MediaType.valueOf("application/javascript")));

        HttpMessageConverter<Object> orisApiQuirksMessageConverter = new OrisApiQuirksHandlingMessageConverter(
                messageConverter);

        RestClient restClient = restClientBuilder.baseUrl("https://oris.orientacnisporty.cz")
                .configureMessageConverters(c -> {
                    c.addCustomConverter(orisApiQuirksMessageConverter);
                    c.withJsonConverter(new JacksonJsonHttpMessageConverter(objectMapperBuilder));
                })
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
        LOG.warn("Request {} {}", request.getMethod(), request.getURI());

        var response = execution.execute(request, body);

        LOG.warn("Response status {}", response.getStatusCode());

        return response;
    }
}


class OrisApiQuirksHandlingMessageConverter implements SmartHttpMessageConverter<Object> {
    private static final Logger log = LoggerFactory.getLogger(OrisApiQuirksHandlingMessageConverter.class);
    private final SmartHttpMessageConverter<Object> delegate;

    OrisApiQuirksHandlingMessageConverter(SmartHttpMessageConverter<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean canRead(ResolvableType type, @Nullable MediaType mediaType) {
        return delegate.canRead(type, mediaType);
    }

    @Override
    public Object read(ResolvableType type, HttpInputMessage inputMessage, @Nullable Map<String, Object> hints) throws IOException, HttpMessageNotReadableException {
        inputMessage.getBody().mark(Integer.MAX_VALUE);

        Object result = delegate.read(type, inputMessage, hints);

        // ORIS respond with HTTP 200 and empty "data" attribute in the response instead of HTTP 404. Convert it to HTTP 404 exception so our clients can be "normal"
        if (result instanceof OrisApiClient.OrisResponse<?> typedResponse) {
            if (typedResponse.data() == null) {
                log.debug(
                        "Oris responded with HTTP 200 + empty 'data' attribute meaning data were not found. Converting to HTTP 404 error");
                throw HttpClientErrorException.create(HttpStatusCode.valueOf(404),
                        "NOT_FOUND",
                        new HttpHeaders(),
                        inputMessage.getBody().readAllBytes(),
                        StandardCharsets.UTF_8);
            }
        }
        return result;
    }

    @Override
    public boolean canWrite(ResolvableType targetType, Class<?> valueClass, @Nullable MediaType mediaType) {
        return delegate.canWrite(targetType, valueClass, mediaType);
    }

    @Override
    public void write(Object o, ResolvableType type, @Nullable MediaType contentType, HttpOutputMessage outputMessage, @Nullable Map<String, Object> hints) throws IOException, HttpMessageNotWritableException {
        delegate.write(o, type, contentType, outputMessage, hints);
    }


    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return delegate.getSupportedMediaTypes();
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
            }
            return true;
        }
        return false;
    }
}