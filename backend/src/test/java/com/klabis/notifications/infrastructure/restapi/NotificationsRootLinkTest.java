package com.klabis.notifications.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.notifications.NotificationsWebMvcTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Links contributed by notifications to the API root")
class NotificationsRootLinkTest {

    @Nested
    @NotificationsWebMvcTest
    @TestPropertySource(properties = {
            "klabis.notifications.vapid.public-key=BTestPublicKey",
            "klabis.notifications.vapid.private-key=testPrivateKey"
    })
    @DisplayName("when VAPID keys are configured")
    class Configured {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @WithKlabisMockUser
        @DisplayName("links the notification settings")
        void linksSettings() throws Exception {
            mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.notifications.href", endsWith("/api/notifications")));
        }
    }

    @Nested
    @NotificationsWebMvcTest
    @TestPropertySource(properties = {
            "klabis.notifications.vapid.public-key=",
            "klabis.notifications.vapid.private-key="
    })
    @DisplayName("when VAPID keys are not configured")
    class NotConfigured {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @WithKlabisMockUser
        @DisplayName("omits the notifications link")
        void omitsLink() throws Exception {
            mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._links.notifications").doesNotExist());
        }
    }
}
