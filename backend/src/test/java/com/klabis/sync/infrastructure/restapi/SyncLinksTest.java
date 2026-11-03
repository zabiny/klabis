package com.klabis.sync.infrastructure.restapi;

import com.klabis.common.WithKlabisMockUser;
import com.klabis.common.users.Authority;
import com.klabis.sync.SyncWebMvcTest;
import com.klabis.sync.domain.SyncPairing;
import com.klabis.sync.application.SyncPairingLookup;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("SyncLinks")
@SyncWebMvcTest
class SyncLinksTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SyncLinks syncLinks;

    @Autowired
    private SyncPairingLookup pairingLookup;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, webApplicationContext);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @ParameterizedTest
    @EnumSource(SyncEntityType.class)
    @DisplayName("adds sync link for ACTIVE pairing")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void activeAddsLink(SyncEntityType type) {
        assertSyncLink(type, SyncPairing.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(SyncEntityType.class)
    @DisplayName("adds sync link for RETIRED pairing")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void retiredAddsLink(SyncEntityType type) {
        assertSyncLink(type, SyncPairing.RETIRED);
    }

    @ParameterizedTest
    @EnumSource(SyncEntityType.class)
    @DisplayName("adds nothing for NONE pairing")
    @WithKlabisMockUser(authorities = {Authority.SYNC_MANAGE})
    void noneAddsNothing(SyncEntityType type) {
        SyncTarget target = new SyncTarget(type, "id-1");
        when(pairingLookup.pairingOf(target)).thenReturn(SyncPairing.NONE);
        RepresentationModel<?> model = new RepresentationModel<>();

        syncLinks.addTo(model, target);

        assertThat(model.getLinks()).isEmpty();
    }

    private void assertSyncLink(SyncEntityType type, SyncPairing pairing) {
        SyncTarget target = new SyncTarget(type, "id-1");
        when(pairingLookup.pairingOf(target)).thenReturn(pairing);
        RepresentationModel<?> model = new RepresentationModel<>();

        syncLinks.addTo(model, target);

        assertThat(model.getLink("sync")).map(Link::getHref)
                .hasValueSatisfying(href -> assertThat(href).endsWith("/api/" + type.pathSegment() + "/id-1/sync"));
    }
}
