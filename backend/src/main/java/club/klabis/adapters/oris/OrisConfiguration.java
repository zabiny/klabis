package club.klabis.adapters.oris;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
class OrisConfiguration implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OrisConfiguration.class);



    @Bean
    OrisApiClient orisApiClient(RestClient.Builder restClientBuilder) {

        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setSupportedMediaTypes(List.of(MediaType.valueOf("application/javascript")));

        GenericHttpMessageConverter<Object> orisApiQuirksMessageConverter = new OrisApiQuirksHandlingMessageConverter(messageConverter);

        RestClient restClient = restClientBuilder.baseUrl("https://oris.orientacnisporty.cz")
                .messageConverters(c -> c.add(orisApiQuirksMessageConverter))
                .requestInterceptor(this)
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

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


class OrisApiQuirksHandlingMessageConverter implements GenericHttpMessageConverter<Object> {
    private final GenericHttpMessageConverter<Object> delegate;

    OrisApiQuirksHandlingMessageConverter(GenericHttpMessageConverter<Object> delegate) {
        this.delegate = delegate;
    }

    private boolean isQuirkParsingErrorForSingleItemNotFoundResponseWithWrongDataPayloadAsArray(HttpMessageNotReadableException ex) {
        // for ex "JSON parse error: Cannot deserialize value of type `club.klabis.adapters.oris.OrisApiClient$OrisUserInfo` from Array value (token `JsonToken.START_ARRAY`)".equals(ex.getMessage());
        return ex.getMessage().startsWith("JSON parse error: Cannot deserialize value of type `") && ex.getMessage().endsWith("` from Array value (token `JsonToken.START_ARRAY`)");
    }

    @Override
    public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        inputMessage.getBody().mark(Integer.MAX_VALUE);
        try {
            return delegate.read(type, contextClass, inputMessage);
        } catch (HttpMessageNotReadableException ex) {
            // any better way how to detect that ORIS returns "item not found" response (HTTP 200 + data with array) instead of expected object?
            if (isQuirkParsingErrorForSingleItemNotFoundResponseWithWrongDataPayloadAsArray(ex)) {
                throw HttpClientErrorException.create(HttpStatusCode.valueOf(404), "NOT_FOUND", new HttpHeaders(), inputMessage.getBody().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                throw ex;
            }
        }
    }

    @Override
    public Object read(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return delegate.read(clazz, inputMessage);
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        return delegate.canRead(type, contextClass, mediaType);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return delegate.canRead(clazz, mediaType);
    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        return delegate.canWrite(type, clazz, mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return delegate.canWrite(clazz, mediaType);
    }

    @Override
    public void write(Object o, Type type, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        delegate.write(o, type, contentType, outputMessage);
    }

    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        delegate.write(o, contentType, outputMessage);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return delegate.getSupportedMediaTypes();
    }

    @Override
    public List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
        return delegate.getSupportedMediaTypes(clazz);
    }
}