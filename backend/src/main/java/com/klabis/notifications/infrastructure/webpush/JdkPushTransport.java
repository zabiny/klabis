package com.klabis.notifications.infrastructure.webpush;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

class JdkPushTransport implements PushTransport {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    @Override
    public int post(HttpRequest request) throws IOException, InterruptedException {
        HttpRequest withTimeout = HttpRequest.newBuilder(request, (name, value) -> true)
                .timeout(REQUEST_TIMEOUT)
                .build();
        return httpClient.send(withTimeout, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
