package com.klabis.notifications.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.UserId;
import com.klabis.notifications.NotificationDeviceTestDataBuilder;
import com.klabis.notifications.NotificationsWebMvcTest;
import com.klabis.notifications.application.NotificationDevicesPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
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
}
