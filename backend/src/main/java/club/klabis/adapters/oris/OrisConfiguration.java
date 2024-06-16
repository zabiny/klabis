package club.klabis.adapters.oris;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.util.List;

@Configuration
class OrisConfiguration implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OrisConfiguration.class);



    @Bean
    OrisService orisServiceClient(RestClient.Builder restClientBuilder) {

        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setSupportedMediaTypes(List.of(MediaType.valueOf("application/javascript")));

        RestClient restClient = restClientBuilder.baseUrl("https://oris.orientacnisporty.cz")
                .messageConverters(c -> c.add(messageConverter))
                .requestInterceptor(this)
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(OrisService.class);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        LOG.warn("Request {} {}", request.getMethod(), request.getURI());

        var response = execution.execute(request, body);

        LOG.warn("Response status {}", response.getStatusCode());

        return response;
    }
}
