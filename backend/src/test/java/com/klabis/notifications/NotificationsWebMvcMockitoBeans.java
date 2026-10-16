package com.klabis.notifications;

import com.klabis.notifications.application.NotificationDevicesPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@MockitoBean(types = {
        NotificationDevicesPort.class,
        PushNotifications.class
})
public @interface NotificationsWebMvcMockitoBeans {
}
