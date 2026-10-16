package com.klabis.notifications.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.UserId;
import com.klabis.notifications.NotificationDeviceTestDataBuilder;
import com.klabis.notifications.NotificationsWebMvcTest;
import com.klabis.notifications.PushMessage;
import com.klabis.notifications.PushNotifications;
import com.klabis.notifications.application.NotificationDevicesPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import com.klabis.notifications.domain.PushEndpoint;
import com.klabis.notifications.domain.PushKeys;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("NotificationController API tests")
@NotificationsWebMvcTest
@TestPropertySource(properties = {
        "klabis.notifications.vapid.public-key=BTestPublicKey",
        "klabis.notifications.vapid.private-key=testPrivateKey"
})
class NotificationControllerTest {

    private static final String USER_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationDevicesPort devicesPort;

    @Autowired
    private PushNotifications pushNotifications;

    @Test
    @WithKlabisMockUser(userId = USER_ID)
    @DisplayName("GET /api/notifications returns VAPID key, own devices and self link")
    void returnsOwnDevices() throws Exception {
        UserId currentUser = new UserId(UUID.fromString(USER_ID));
        var device = NotificationDeviceTestDataBuilder.aDevice()
                .withOwner(currentUser)
                .withEndpoint("https://push.example.org/send/own")
                .withDescription("Chrome · Android")
                .build();
        when(devicesPort.listDevices(currentUser)).thenReturn(List.of(device));

        mockMvc.perform(get("/api/notifications").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vapidPublicKey").value("BTestPublicKey"))
                .andExpect(jsonPath("$.devices", hasSize(1)))
                .andExpect(jsonPath("$.devices[0].id").value(device.getId().value().toString()))
                .andExpect(jsonPath("$.devices[0].description").value("Chrome · Android"))
                .andExpect(jsonPath("$.devices[0].endpoint").value("https://push.example.org/send/own"))
                .andExpect(jsonPath("$.devices[0].enabledAt").exists())
                .andExpect(jsonPath("$._links.self.href", endsWith("/api/notifications")));

        verify(devicesPort).listDevices(currentUser);
    }

    @Test
    @WithKlabisMockUser(userId = USER_ID)
    @DisplayName("GET /api/notifications queries devices of the current user only")
    void neverQueriesOtherUsers() throws Exception {
        UserId otherUser = new UserId(UUID.randomUUID());
        when(devicesPort.listDevices(otherUser)).thenReturn(List.of(
                NotificationDeviceTestDataBuilder.aDevice().withOwner(otherUser).build()));

        mockMvc.perform(get("/api/notifications").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.devices", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/notifications is not available to anonymous callers")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/notifications").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithKlabisMockUser(userId = USER_ID)
    @DisplayName("GET /api/notifications offers the registerNotificationDevice template")
    void offersRegisterTemplate() throws Exception {
        mockMvc.perform(get("/api/notifications").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates", hasKey("registerNotificationDevice")))
                .andExpect(jsonPath("$._templates.registerNotificationDevice.method").value("POST"))
                .andExpect(jsonPath("$._templates.registerNotificationDevice.target", endsWith("/api/notifications/devices")));
    }

    private static final String CHROME_UA = "Mozilla/5.0 (Linux; Android 14) Chrome/120.0.0.0 Mobile Safari/537.36";

    private static String body(String endpoint, String p256dh, String auth) {
        return """
                {"endpoint": %s, "p256dh": %s, "auth": %s}
                """.formatted(quote(endpoint), quote(p256dh), quote(auth));
    }

    private static String quote(String v) {
        return v == null ? "null" : "\"" + v + "\"";
    }

    @Test
    @WithKlabisMockUser(userId = USER_ID)
    @DisplayName("POST /api/notifications/devices registers the device and returns 201 with Location")
    void registersDevice() throws Exception {
        UserId currentUser = new UserId(UUID.fromString(USER_ID));
        var device = NotificationDeviceTestDataBuilder.aDevice().withOwner(currentUser).build();
        when(devicesPort.registerDevice(any())).thenReturn(device);

        mockMvc.perform(post("/api/notifications/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .header("User-Agent", CHROME_UA)
                        .content(body("https://push.example.org/send/1", "pk", "au")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/notifications/devices/" + device.getId().value())));

        verify(devicesPort).registerDevice(new NotificationDevicesPort.RegisterDeviceCommand(
                currentUser, new PushEndpoint("https://push.example.org/send/1"), new PushKeys("pk", "au"), CHROME_UA));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://push.example.org/send/1",
            "not a uri",
    })
    @WithKlabisMockUser(userId = USER_ID)
    @DisplayName("POST /api/notifications/devices rejects non-https endpoint")
    void rejectsNonHttps(String endpoint) throws Exception {
        mockMvc.perform(post("/api/notifications/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content(body(endpoint, "pk", "au")))
                .andExpect(status().isBadRequest());

        verify(devicesPort, never()).registerDevice(any());
    }

    @Test
    @WithKlabisMockUser(userId = USER_ID)
    @DisplayName("POST /api/notifications/devices rejects too long values")
    void rejectsTooLongValues() throws Exception {
        String longEndpoint = "https://push.example.org/" + "a".repeat(1000);
        for (String payload : new String[]{
                body(longEndpoint, "pk", "au"),
                body("https://push.example.org/1", "p".repeat(201), "au"),
                body("https://push.example.org/1", "pk", "a".repeat(101))}) {
            mockMvc.perform(post("/api/notifications/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        verify(devicesPort, never()).registerDevice(any());
    }

    @Test
    @WithKlabisMockUser(userId = USER_ID)
    @DisplayName("POST /api/notifications/devices rejects missing fields")
    void rejectsMissingFields() throws Exception {
        for (String payload : new String[]{
                body(null, "pk", "au"),
                body("https://push.example.org/1", null, "au"),
                body("https://push.example.org/1", "pk", null),
                "{}"}) {
            mockMvc.perform(post("/api/notifications/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                            .content(payload))
                    .andExpect(status().isBadRequest());
        }

        verify(devicesPort, never()).registerDevice(any());
    }

    @Test
    @DisplayName("POST /api/notifications/devices is not available to anonymous callers")
    void registerRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/notifications/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaTypes.HAL_FORMS_JSON_VALUE)
                        .content(body("https://push.example.org/1", "pk", "au")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithKlabisMockUser(userId = USER_ID)
    @DisplayName("GET /api/notifications offers sendTestNotification only when the user has a device")
    void offersTestTemplateOnlyWithDevice() throws Exception {
        UserId currentUser = new UserId(UUID.fromString(USER_ID));
        when(devicesPort.listDevices(currentUser)).thenReturn(List.of(
                NotificationDeviceTestDataBuilder.aDevice().withOwner(currentUser).build()));

        mockMvc.perform(get("/api/notifications").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates", hasKey("sendTestNotification")))
                .andExpect(jsonPath("$._templates.sendTestNotification.method").value("POST"))
                .andExpect(jsonPath("$._templates.sendTestNotification.target", endsWith("/api/notifications/test")));
    }

    @Test
    @WithKlabisMockUser(userId = USER_ID)
    @DisplayName("GET /api/notifications hides sendTestNotification when the user has no device")
    void hidesTestTemplateWithoutDevice() throws Exception {
        UserId currentUser = new UserId(UUID.fromString(USER_ID));
        when(devicesPort.listDevices(currentUser)).thenReturn(List.of());

        mockMvc.perform(get("/api/notifications").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._templates", not(hasKey("sendTestNotification"))));
    }

    @Test
    @WithKlabisMockUser(userId = USER_ID)
    @DisplayName("POST /api/notifications/test sends the test notification to the current user")
    void sendsTestNotification() throws Exception {
        UserId currentUser = new UserId(UUID.fromString(USER_ID));

        mockMvc.perform(post("/api/notifications/test").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isNoContent());

        verify(pushNotifications).send(currentUser, new PushMessage("Notifikace fungují",
                "Na tomto zařízení budete dostávat oznámení z Klabisu.", "/notifications"));
    }

    @Test
    @DisplayName("POST /api/notifications/test is not available to anonymous callers")
    void testNotificationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/notifications/test").accept(MediaTypes.HAL_FORMS_JSON_VALUE))
                .andExpect(status().isUnauthorized());
        verify(pushNotifications, never()).send(any(), any());
    }
}
