package com.klabis.notifications.infrastructure.webpush;

import com.klabis.notifications.PushNotifications;
import com.klabis.notifications.application.NotificationDevicesPort;
import com.klabis.notifications.application.VapidProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
class WebPushConfiguration {

    @Bean
    @Conditional(VapidKeysConfigured.class)
    WebPushDelivery webPushDelivery(VapidProperties properties, NotificationDevicesPort devicesPort,
                                    ObjectMapper objectMapper) {
        return new WebPushDelivery(devicesPort, new WebPushSender(properties, new JdkPushTransport(), objectMapper));
    }

    @Bean
    PushNotifications pushNotifications(ObjectProvider<WebPushDelivery> delivery) {
        WebPushDelivery webPushDelivery = delivery.getIfAvailable();
        return webPushDelivery == null ? new NoOpPushNotifications() : new WebPushNotifications(webPushDelivery);
    }

    static class VapidKeysConfigured implements Condition {

        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext context,
                               org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            var environment = context.getEnvironment();
            return StringUtils.hasText(environment.getProperty("klabis.notifications.vapid.public-key"))
                    && StringUtils.hasText(environment.getProperty("klabis.notifications.vapid.private-key"));
        }
    }
}
