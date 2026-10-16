package com.klabis.notifications.infrastructure.webpush;

import java.io.IOException;
import java.net.http.HttpRequest;

interface PushTransport {

    int post(HttpRequest request) throws IOException, InterruptedException;
}
