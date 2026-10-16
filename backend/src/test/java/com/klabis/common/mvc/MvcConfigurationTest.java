package com.klabis.common.mvc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("MvcConfiguration")
class MvcConfigurationTest {

    @Test
    @DisplayName("maps .webmanifest to application/manifest+json in the servlet container")
    void mapsWebmanifestMimeType() {
        WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> customizer =
                new MvcConfiguration().pwaMimeMappingsCustomizer();
        ConfigurableServletWebServerFactory factory = mock(ConfigurableServletWebServerFactory.class);

        customizer.customize(factory);

        ArgumentCaptor<MimeMappings> captor = ArgumentCaptor.forClass(MimeMappings.class);
        verify(factory).setMimeMappings(captor.capture());
        assertThat(captor.getValue().get("webmanifest")).isEqualTo("application/manifest+json");
    }
}
