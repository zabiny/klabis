package com.klabis.common.ui;

import com.klabis.TestApplicationConfiguration;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the SPA fallback / static-content routing chain.
 *
 * <p>Verifies the contract from the {@code non-functional-requirements} spec
 * (Reserved URL Paths Not Captured by SPA Fallback): browser navigation to
 * Swagger UI, OpenAPI document, developer manual, and OIDC silent-renew
 * resources must reach their dedicated handler instead of the SPA shell,
 * while unknown HTML routes do fall back to the SPA index for client-side
 * routing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@DisplayName("Web content routing: SPA fallback vs. reserved paths")
class WebContentRoutingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServletContext servletContext;

    @BeforeEach
    void registerContainerMimeMappings() {
        // The embedded container maps .webmanifest -> application/manifest+json
        // through MvcConfiguration.pwaMimeMappingsCustomizer. MockMvc uses a MockServletContext
        // that does not receive those container mappings, so mirror it here.
        if (servletContext instanceof MockServletContext mockServletContext) {
            mockServletContext.addMimeType("webmanifest", MediaType.parseMediaType("application/manifest+json"));
        }
    }

    @Test
    @DisplayName("authenticated /docs/index.html serves the developer manual, not the SPA shell")
    void docsServesDeveloperManualAfterSessionLogin() throws Exception {
        MvcResult result = mockMvc.perform(get("/docs/index.html")
                        .with(user("admin").roles("USER"))
                        .accept(MediaType.TEXT_HTML))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        String forwarded = result.getResponse().getForwardedUrl();

        // After possible forward to the static resource, status should be 200 and body
        // must contain a marker that is unique to the developer manual page (not SPA shell).
        assertThat(status).isEqualTo(200);
        assertThat(forwarded).isNotEqualTo("/index.html");
        assertThat(body).contains("Dokumentace Klabis");
        assertThat(body).doesNotContain("<div id=\"root\">");
    }

    @Test
    @DisplayName("/swagger-ui.html redirects into Swagger UI without auth (baseline)")
    void swaggerUiHtmlRedirects() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("/v3/api-docs returns OpenAPI JSON (baseline)")
    void openApiDocsAreJson() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("/silent-renew.html returns the OIDC silent-renew page (baseline)")
    void silentRenewHtmlIsReachable() throws Exception {
        MvcResult result = mockMvc.perform(get("/silent-renew.html")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentAsString()).contains("Silent Renew");
    }

    @Test
    @DisplayName("SPA route /events serves the React shell (baseline)")
    void spaRouteServesIndexHtml() throws Exception {
        MvcResult result = mockMvc.perform(get("/events")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        String forwarded = result.getResponse().getForwardedUrl();

        // Either the dispatcher forwards to /index.html (filter-based fallback) or
        // the SPA controller forwards directly. Both render the React shell.
        assertThat(status).isEqualTo(200);
        boolean rendersSpaShell = body.contains("<div id=\"root\">")
                || "/index.html".equals(forwarded);
        assertThat(rendersSpaShell)
                .as("SPA route must render the React shell (body or forward to /index.html)")
                .isTrue();
    }

    @Test
    @DisplayName("/api/foo-bar-not-existing with Accept: application/json returns JSON 404, not SPA shell")
    void unknownApiPathReturnsJsonNotSpaShell() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/foo-bar-not-existing")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        String forwarded = result.getResponse().getForwardedUrl();

        // Spring Security on the API chain returns 401 for unauthenticated JSON requests
        // (no JWT). What matters here: it must NOT be forwarded to /index.html.
        assertThat(forwarded).isNotEqualTo("/index.html");
        assertThat(body).doesNotContain("<div id=\"root\">");
        assertThat(status).isIn(401, 403, 404);
    }

    @Test
    @DisplayName("SPA filter does not touch a 200 HTML response from another handler")
    void filterPreservesSuccessfulHtmlResponses() throws Exception {
        // /silent-renew.html is served by ResourceHttpRequestHandler with status 200.
        // The filter must NOT forward to /index.html in this case — body must be the
        // silent-renew page, not the SPA shell.
        MvcResult result = mockMvc.perform(get("/silent-renew.html")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getForwardedUrl()).isNotEqualTo("/index.html");
        assertThat(result.getResponse().getContentAsString()).contains("Silent Renew");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("<div id=\"root\">");
    }

    @Test
    @DisplayName("GET /login renders the SPA shell (React-based login form)")
    void loginPageRendersSpaShell() throws Exception {
        // The login page is a React route — Spring Security's formLogin only handles
        // POST /login (UsernamePasswordAuthenticationFilter); GET /login must fall
        // through to the SPA index.html so the React LoginPage renders.
        MvcResult result = mockMvc.perform(get("/login")
                        .accept(MediaType.TEXT_HTML))
                .andReturn();

        int status = result.getResponse().getStatus();
        String body = result.getResponse().getContentAsString();
        String forwarded = result.getResponse().getForwardedUrl();

        assertThat(status).isEqualTo(200);
        boolean rendersSpaShell = body.contains("<div id=\"root\">")
                || "/index.html".equals(forwarded);
        assertThat(rendersSpaShell)
                .as("GET /login must render the React shell (body contains root div or forward to /index.html)")
                .isTrue();
    }

    @Test
    @DisplayName("SPA filter does not forward to index.html for non-HTML 404s")
    void filterDoesNotForwardJsonAcceptOnUnknownPath() throws Exception {
        // Non-API unknown path with Accept: application/json — must stay as 404 JSON,
        // not be rewritten to SPA shell. Guards against the filter accidentally
        // catching JSON consumers.
        MvcResult result = mockMvc.perform(get("/random-not-existing-thing")
                        .accept(MediaType.APPLICATION_JSON))
                .andReturn();

        String forwarded = result.getResponse().getForwardedUrl();
        String body = result.getResponse().getContentAsString();

        assertThat(forwarded).isNotEqualTo("/index.html");
        assertThat(body).doesNotContain("<div id=\"root\">");
    }

    @Test
    @DisplayName("GET /index.html is served with Cache-Control: no-cache")
    void indexHtmlHasNoCache() throws Exception {
        mockMvc.perform(get("/index.html").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    @DisplayName("GET / (SPA welcome page) is served with Cache-Control: no-cache")
    void rootHasNoCache() throws Exception {
        mockMvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    @DisplayName("GET an SPA route is served with Cache-Control: no-cache")
    void spaRouteHasNoCache() throws Exception {
        mockMvc.perform(get("/members").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    @DisplayName("GET /auth/callback is served with Cache-Control: no-cache")
    void authCallbackHasNoCache() throws Exception {
        mockMvc.perform(get("/auth/callback").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    @DisplayName("GET /silent-renew.html is served with Cache-Control: no-cache")
    void silentRenewHasNoCache() throws Exception {
        mockMvc.perform(get("/silent-renew.html").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-cache")));
    }

    @Test
    @DisplayName("hashed /assets/** keep the immutable one-year policy")
    void hashedAssetsAreImmutable() throws Exception {
        mockMvc.perform(get(firstAssetPath()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
                .andExpect(header().string("Cache-Control", containsString("immutable")));
    }

    @Test
    @DisplayName("non-hashed static files keep the one-hour public policy")
    void plainStaticFilesKeepOneHourCache() throws Exception {
        mockMvc.perform(get("/favicon.svg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("max-age=3600")));
    }

    @Test
    @DisplayName("manifest is served in every profile with application/manifest+json and no-cache")
    void manifestServedWithoutPwaProfile() throws Exception {
        mockMvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("application/manifest+json")))
                .andExpect(header().string("Cache-Control", containsString("no-cache")))
                .andExpect(header().string("Cache-Control", containsString("must-revalidate")));
    }

    @Test
    @DisplayName("service worker is served in every profile with no-cache, must-revalidate")
    void serviceWorkerServedWithoutPwaProfile() throws Exception {
        mockMvc.perform(get("/sw.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-cache")))
                .andExpect(header().string("Cache-Control", containsString("must-revalidate")));
    }

    private static String firstAssetPath() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:/static/assets/*.js");
        assertThat(resources).as("at least one hashed JS asset must be published").isNotEmpty();
        return "/assets/" + resources[0].getFilename();
    }
}
