package com.klabis.notifications.infrastructure.webpush;

import com.klabis.notifications.PushNotifications;
import com.klabis.notifications.application.NotificationDevicesPort;
import com.klabis.notifications.application.VapidProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
class WebPushConfiguration {

    @Bean
    PushNotifications pushNotifications(VapidProperties properties, NotificationDevicesPort devicesPort,
                                        @Qualifier("applicationTaskExecutor") ObjectProvider<Executor> executor,
                                        ObjectMapper objectMapper) {
        if (!properties.isConfigured()) {
            return new NoOpPushNotifications();
        }
        return new WebPushNotifications(executor.getIfAvailable(Executors::newVirtualThreadPerTaskExecutor), devicesPort,
                new WebPushSender(properties, new JdkPushTransport(), objectMapper));
    }
}
